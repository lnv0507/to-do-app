package vn.com.anhemsoftware.license_app.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.entity.UserDevice;
import vn.com.anhemsoftware.license_app.exception.DeviceVerificationRequiredException;
import vn.com.anhemsoftware.license_app.mapper.UserMapper;
import vn.com.anhemsoftware.license_app.payload.auth.internal.OtpCacheDto;
import vn.com.anhemsoftware.license_app.payload.auth.internal.PendingLoginCache;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyDeviceRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
import vn.com.anhemsoftware.license_app.repository.UserDeviceRepository;
import vn.com.anhemsoftware.license_app.repository.UserRepository;
import vn.com.anhemsoftware.license_app.service.AuthService;
import vn.com.anhemsoftware.license_app.service.JWTService;
import vn.com.anhemsoftware.license_app.util.CookieUtil;
import vn.com.anhemsoftware.license_app.util.IpSubnetUtil;
import vn.com.anhemsoftware.license_app.util.OTPGenerator;
import vn.com.anhemsoftware.license_app.util.RequestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ─── Redis key prefixes ────────────────────────────────────────────────────
    private static final String REDIS_REFRESH = "REFRESH:";
    private static final String REDIS_USER_SESSIONS = "USER_SESSIONS:";
    private static final String REDIS_ACTION_TOKEN = "ACTION_TOKEN:";
    /** Pending HIGH-risk login waiting for OTP — TTL 10 minutes */
    private static final String REDIS_PENDING_LOGIN = "PENDING_LOGIN:";

    // ─── TTL constants ─────────────────────────────────────────────────────────
    private static final long REFRESH_TTL_DAYS = 7;
    private static final long DEVICE_COOKIE_DAYS = 365 * 5L;
    private static final long ACTION_TOKEN_HOURS = 24;
    private static final long PENDING_LOGIN_MINUTES = 10;

    // ─── Dependencies ──────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserDeviceRepository userDeviceRepository;

    // ═══════════════════════════════════════════════════════════════════════════
    // SIGN UP
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void signUp(SignUpRequest signUpRequest) throws Exception {
        if (userRepository.findByEmail(signUpRequest.email()).isPresent()) {
            throw new Exception("Email already exists");
        }
        String otp = OTPGenerator.generateOTP();
        OtpCacheDto otpCacheDto = new OtpCacheDto(signUpRequest, otp);
        redisTemplate.opsForValue().set("REG:" + signUpRequest.email(), otpCacheDto, 5, TimeUnit.MINUTES);
        emailService.sendEmail(signUpRequest.email(), "OTP", otp);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIRM OTP (registration)
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public ResponseEntity<SignUpResponse> confirmOtp(
            HttpServletRequest request,
            HttpServletResponse response,
            @Valid VerifyOtpRequest verifyOtpRequest) throws Exception {

        if (redisTemplate.opsForValue().get("REG:" + verifyOtpRequest.email()) == null) {
            throw new Exception("OTP is not correct");
        }
        OtpCacheDto cacheData = (OtpCacheDto) redisTemplate.opsForValue().get("REG:" + verifyOtpRequest.email());
        assert cacheData != null;
        if (!cacheData.otp().equals(verifyOtpRequest.otp())) {
            throw new Exception("OTP is not correct");
        }
        redisTemplate.delete("REG:" + verifyOtpRequest.email());

        User userEntity = userMapper.toUser(cacheData.signUpRequest());
        userEntity.setPassword(passwordEncoder.encode(cacheData.signUpRequest().password()));
        User userDB = userRepository.save(userEntity);

        // First device from registration → trusted immediately
        UUID refreshTokenId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(userDB.getEmail(), refreshTokenId);
        String refreshToken = jwtService.generateRefreshToken(userDB.getEmail(), refreshTokenId);
        long maxAgeSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TTL_DAYS);

        setAuthCookies(response, refreshToken, maxAgeSeconds);
        redisTemplate.opsForValue().set(REDIS_REFRESH + refreshTokenId, "ACTIVE", maxAgeSeconds, TimeUnit.SECONDS);
        saveUserSession(userDB.getId(), refreshTokenId.toString(), maxAgeSeconds, request);

        String newDeviceId = UUID.randomUUID().toString();
        UserDevice userDevice = UserDevice.builder()
                .user(userDB)
                .deviceId(newDeviceId)
                .isTrusted(true)
                .sessionJti(refreshTokenId.toString())
                .userAgent(request.getHeader("User-Agent"))
                .lastActive(System.currentTimeMillis())
                .lastIp(RequestUtils.getClientIp(request))
                .createdAt(System.currentTimeMillis())
                .build();
        userDeviceRepository.save(userDevice);
        setDeviceIdCookie(response, newDeviceId, TimeUnit.DAYS.toSeconds(DEVICE_COOKIE_DAYS));

        return ResponseEntity.ok(new SignUpResponse(accessToken));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIGN IN — RBA Engine
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public ResponseEntity<SignUpResponse> signIn(HttpServletResponse response,
            @Valid SignInRequest signInRequest,
            HttpServletRequest request) throws Exception {
        // 1. Validate credentials
        User userDB = userRepository.findByEmail(signInRequest.email())
                .orElseThrow(() -> new Exception("User not found"));
        if (!passwordEncoder.matches(signInRequest.password(), userDB.getPassword())) {
            throw new IllegalArgumentException("Password not match");
        }

        // 2. RBA evaluation — HIGH risk sẽ throw exception trước khi cấp session
        RiskLevel risk = evaluateRisk(userDB, request);
        if (risk == RiskLevel.HIGH) {
            handleHighRisk(userDB, request); // throws DeviceVerificationRequiredException
        }

        // 3. Tạo session (chỉ NONE / LOW / MEDIUM mới đến được đây)
        UUID refreshTokenId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(userDB.getEmail(), refreshTokenId);
        String refreshToken = jwtService.generateRefreshToken(userDB.getEmail(), refreshTokenId);
        long maxAgeSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TTL_DAYS);

        setAuthCookies(response, refreshToken, maxAgeSeconds);
        redisTemplate.opsForValue().set(REDIS_REFRESH + refreshTokenId, "ACTIVE", maxAgeSeconds, TimeUnit.SECONDS);
        saveUserSession(userDB.getId(), refreshTokenId.toString(), maxAgeSeconds, request);

        // 4. Handle device by risk level
        handleDeviceByRisk(risk, userDB, request, response, refreshTokenId);

        return ResponseEntity.ok(new SignUpResponse(accessToken));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFY DEVICE — Phase 2: OTP verification after HIGH risk block
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public ResponseEntity<SignUpResponse> verifyDevice(HttpServletRequest request,
            HttpServletResponse response,
            VerifyDeviceRequest verifyDeviceRequest) throws Exception {
        // 1. Get pending context from Redis
        String key = REDIS_PENDING_LOGIN + verifyDeviceRequest.verificationToken();
        PendingLoginCache pending = (PendingLoginCache) redisTemplate.opsForValue().get(key);
        if (pending == null) {
            throw new Exception("Verification code does not exist or has expired (10 minutes).");
        }

        // 2. Check OTP
        if (!pending.otp().equals(verifyDeviceRequest.otp())) {
            throw new IllegalArgumentException("Incorrect OTP.");
        }

        // 3. Delete one-time key
        redisTemplate.delete(key);

        // 4. Find user by email
        User userDB = userRepository.findByEmail(pending.email())
                .orElseThrow(() -> new Exception("User not found"));

        // 5. Create full session
        UUID refreshTokenId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(userDB.getEmail(), refreshTokenId);
        String refreshToken = jwtService.generateRefreshToken(userDB.getEmail(), refreshTokenId);
        long maxAgeSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TTL_DAYS);

        setAuthCookies(response, refreshToken, maxAgeSeconds);
        redisTemplate.opsForValue().set(REDIS_REFRESH + refreshTokenId, "ACTIVE", maxAgeSeconds, TimeUnit.SECONDS);
        saveUserSession(userDB.getId(), refreshTokenId.toString(), maxAgeSeconds, request);

        // 6. Create new UserDevice with isTrusted=true (OTP verified = trusted)
        String newDeviceId = UUID.randomUUID().toString();
        UserDevice newDevice = UserDevice.builder()
                .user(userDB)
                .deviceId(newDeviceId)
                .isTrusted(true)
                .sessionJti(refreshTokenId.toString())
                .userAgent(pending.userAgent())
                .lastActive(System.currentTimeMillis())
                .lastIp(pending.ip())
                .createdAt(System.currentTimeMillis())
                .build();
        userDeviceRepository.save(newDevice);
        setDeviceIdCookie(response, newDeviceId, TimeUnit.DAYS.toSeconds(DEVICE_COOKIE_DAYS));

        return ResponseEntity.ok(new SignUpResponse(accessToken));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REFRESH TOKEN
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public ResponseEntity<SignUpResponse> refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String oldRefreshToken = CookieUtil.read(request, "refreshToken");
        if (oldRefreshToken == null)
            throw new Exception("Refresh token is missing");
        if (!jwtService.isTokenValid(oldRefreshToken))
            throw new Exception("Invalid or Expired Refresh Token");

        String oldJti = jwtService.extractJti(oldRefreshToken);
        String email = jwtService.extractEmail(oldRefreshToken);

        String status = (String) redisTemplate.opsForValue().get(REDIS_REFRESH + oldJti);
        if (status == null)
            throw new Exception("Refresh token has been revoked or reused");

        redisTemplate.delete(REDIS_REFRESH + oldJti);
        User userDB = userRepository.findByEmail(email).orElseThrow(() -> new Exception("User not found"));
        redisTemplate.opsForHash().delete(REDIS_USER_SESSIONS + userDB.getId(), oldJti);

        UUID refreshTokenId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(email, refreshTokenId);
        String refreshToken = jwtService.generateRefreshToken(email, refreshTokenId);
        long maxAgeSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TTL_DAYS);

        setAuthCookies(response, refreshToken, maxAgeSeconds);
        redisTemplate.opsForValue().set(REDIS_REFRESH + refreshTokenId, "ACTIVE", maxAgeSeconds, TimeUnit.SECONDS);
        saveUserSession(userDB.getId(), refreshTokenId.toString(), maxAgeSeconds, request);

        return ResponseEntity.ok(new SignUpResponse(accessToken));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOGOUT ALL EXCEPT CURRENT
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void logoutAllExcept(Long userId, String currentJti) {
        String sessionKey = REDIS_USER_SESSIONS + userId;
        Map<Object, Object> sessions = redisTemplate.opsForHash().entries(sessionKey);
        for (Object key : sessions.keySet()) {
            String existingJti = (String) key;
            if (!existingJti.equals(currentJti)) {
                redisTemplate.delete(REDIS_REFRESH + existingJti);
                redisTemplate.opsForHash().delete(sessionKey, existingJti);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REPORT DEVICE — "This is not me"
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void reportDevice(String actionToken) throws Exception {
        String deviceId = (String) redisTemplate.opsForValue().get(REDIS_ACTION_TOKEN + actionToken);
        if (deviceId == null) {
            throw new Exception("Token does not exist or has expired.");
        }
        redisTemplate.delete(REDIS_ACTION_TOKEN + actionToken);

        UserDevice device = userDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new Exception("Device not found."));

        String jti = device.getSessionJti();
        Long userId = device.getUser().getId();
        if (jti != null) {
            redisTemplate.delete(REDIS_REFRESH + jti);
            redisTemplate.opsForHash().delete(REDIS_USER_SESSIONS + userId, jti);
        }
        // Reset isTrusted=false → the next login from this device
        // will be evaluated as MEDIUM (if known IP) or HIGH (if unknown IP) and an
        // email will be sent again
        device.setIsTrusted(false);
        userDeviceRepository.save(device);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RBA ENGINE
    // ═══════════════════════════════════════════════════════════════════════════

    private enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH
    }

    /**
     * Calculate risk level based on device token + subnet.
     * NONE : isTrusted=true + same subnet → allow access
     * LOW : isTrusted=true + different subnet → allow access (reset modem OK)
     * MEDIUM : unknown device + same subnet → allow access + warning email
     * HIGH : unknown device + different subnet → BLOCK + OTP
     */
    private RiskLevel evaluateRisk(User userDB, HttpServletRequest request) {
        String deviceIdCookie = CookieUtil.read(request, "device_id");
        String currentIp = RequestUtils.getClientIp(request);

        // ─── Case: has device cookie ──────────────────────────────────────────
        if (deviceIdCookie != null) {
            UserDevice existing = userDeviceRepository
                    .findByUserIdAndDeviceId(userDB.getId(), deviceIdCookie)
                    .orElse(null);

            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsTrusted())) {
                    // NONE: trusted device + same subnet
                    // LOW : trusted device + IP changed (reset modem)
                    return IpSubnetUtil.isSameSubnet(currentIp, existing.getLastIp())
                            ? RiskLevel.NONE
                            : RiskLevel.LOW;
                }
                // isTrusted=false → known but not trusted (previously reported)
                return IpSubnetUtil.isSameSubnet(currentIp, existing.getLastIp())
                        ? RiskLevel.MEDIUM
                        : RiskLevel.HIGH;
            }
            // Cookie exists but not found in DB → treated as new device
        }

        // ─── Case: no cookie (new device) or invalid cookie ──────────────────
        // Compare IP with ALL known devices of the user
        // → Same subnet as any device: MEDIUM (known IP, new device)
        // → No subnet match: HIGH (unknown IP + unknown device)
        boolean ipKnown = userDeviceRepository.findAllByUserId(userDB.getId())
                .stream()
                .anyMatch(d -> IpSubnetUtil.isSameSubnet(currentIp, d.getLastIp()));

        return ipKnown ? RiskLevel.MEDIUM : RiskLevel.HIGH;
    }

    /**
     * NONE / LOW / MEDIUM: update or create device.
     * HIGH is not called here (thrown earlier in signIn).
     */
    private void handleDeviceByRisk(RiskLevel risk, User userDB, HttpServletRequest request,
            HttpServletResponse response, UUID refreshTokenId) {
        String deviceIdCookie = CookieUtil.read(request, "device_id");
        String currentIp = RequestUtils.getClientIp(request);
        String currentUa = request.getHeader("User-Agent");
        long now = System.currentTimeMillis();

        // NONE & LOW: trusted device already exists — silent update
        if (risk == RiskLevel.NONE || risk == RiskLevel.LOW) {
            UserDevice existing = userDeviceRepository
                    .findByUserIdAndDeviceId(userDB.getId(), deviceIdCookie)
                    .orElse(null);
            if (existing != null) {
                existing.setLastActive(now);
                existing.setLastIp(currentIp);
                existing.setUserAgent(currentUa);
                existing.setSessionJti(refreshTokenId.toString());
                userDeviceRepository.save(existing);
            }
            return;
        }

        // MEDIUM: unknown device + known IP → create new device isTrusted=true +
        // warning email
        // Known IP → high probability of a real user (changed device / reinstalled OS).
        // Trust immediately, but still warn the user.
        // If it is not them, click "This is not me" link → revoke session +
        // isTrusted=false.
        String newDeviceId = UUID.randomUUID().toString();
        UserDevice newDevice = UserDevice.builder()
                .user(userDB)
                .deviceId(newDeviceId)
                .isTrusted(true) // trusted immediately because IP is known
                .sessionJti(refreshTokenId.toString())
                .userAgent(currentUa)
                .lastActive(now)
                .lastIp(currentIp)
                .createdAt(now)
                .build();
        userDeviceRepository.save(newDevice);
        setDeviceIdCookie(response, newDeviceId, TimeUnit.DAYS.toSeconds(DEVICE_COOKIE_DAYS));

        // ActionToken 24h for "This is not me" link
        String actionToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REDIS_ACTION_TOKEN + actionToken, newDeviceId, ACTION_TOKEN_HOURS, TimeUnit.HOURS);

        String reportLink = "http://localhost:8080/api/v1/auth/report-device?token=" + actionToken;
        String emailBody = """
                System detected a login from an untrusted device to your account.

                🖥  Device: %s
                🌐  IP Address: %s
                ⚠️  Risk Level: MEDIUM (known IP but untrusted device)

                If THIS IS NOT YOU, click the link to revoke the session immediately:
                👉 %s

                The link is valid for 24 hours.
                """.formatted(currentUa, currentIp, reportLink);

        emailService.sendEmail(
                userDB.getEmail(),
                "⚠️ Warning: Login from untrusted device",
                emailBody);
    }

    /**
     * HIGH risk: no session created, generate OTP + verificationToken, send email,
     * throw
     * exception.
     * Frontend receives 403 + verificationToken → redirect to OTP entry screen.
     */
    private void handleHighRisk(User userDB, HttpServletRequest request) {
        String currentIp = RequestUtils.getClientIp(request);
        String currentUa = request.getHeader("User-Agent");
        String otp = OTPGenerator.generateOTP();

        String verificationToken = UUID.randomUUID().toString();
        PendingLoginCache pending = new PendingLoginCache(userDB.getEmail(), otp, currentIp, currentUa);
        redisTemplate.opsForValue().set(
                REDIS_PENDING_LOGIN + verificationToken, pending, PENDING_LOGIN_MINUTES, TimeUnit.MINUTES);

        String emailBody = """
                Someone is trying to log in to your account from a completely new device and IP.

                🖥  Device: %s
                🌐  IP Address: %s
                ⚠️  Risk Level: HIGH

                If THIS IS YOU, please enter the following OTP into the application:

                🔐  OTP Code: %s

                The code is valid for 10 minutes.
                If it is not you, please ignore this email. The system has blocked this login.
                """.formatted(currentUa, currentIp, otp);

        emailService.sendEmail(
                userDB.getEmail(),
                "🔐 New Login Verification — Your OTP Code",
                emailBody);

        // Throw → GlobalExceptionHandler returns 403 + verificationToken to Frontend
        throw new DeviceVerificationRequiredException(verificationToken);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private void saveUserSession(Long userId, String jti, long maxAgeSeconds, HttpServletRequest request) {
        String sessionKey = REDIS_USER_SESSIONS + userId;
        Long sessionCount = redisTemplate.opsForHash().size(sessionKey);
        if (sessionCount != null && sessionCount >= 5) {
            Map<Object, Object> sessions = redisTemplate.opsForHash().entries(sessionKey);
            for (Object key : sessions.keySet()) {
                String existingJti = (String) key;
                if (redisTemplate.opsForValue().get(REDIS_REFRESH + existingJti) == null) {
                    redisTemplate.opsForHash().delete(sessionKey, existingJti);
                }
            }
        }
        String ip = RequestUtils.getClientIp(request);
        UserSessionRecord detail = new UserSessionRecord(ip, System.currentTimeMillis());
        redisTemplate.opsForHash().put(sessionKey, jti, detail);
        redisTemplate.expire(sessionKey, maxAgeSeconds, TimeUnit.SECONDS);
    }

    private void setAuthCookies(HttpServletResponse response, String refreshToken, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void setDeviceIdCookie(HttpServletResponse response, String deviceId, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("device_id", deviceId)
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record UserSessionRecord(String ip, long createdAt) {
    }
}
