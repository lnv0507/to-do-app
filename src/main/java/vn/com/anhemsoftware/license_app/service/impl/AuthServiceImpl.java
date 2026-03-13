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
import vn.com.anhemsoftware.license_app.service.EmailService;
import vn.com.anhemsoftware.license_app.service.JWTService;
import vn.com.anhemsoftware.license_app.util.CookieUtil;
import vn.com.anhemsoftware.license_app.util.IpSubnetUtil;
import vn.com.anhemsoftware.license_app.util.OTPGenerator;
import vn.com.anhemsoftware.license_app.util.RequestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import vn.com.anhemsoftware.license_app.service.EmailService;
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
    /** Pending HIGH-risk login chờ OTP — TTL 10 phút */
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
    // CONFIRM OTP (đăng ký)
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

        // Thiết bị đầu tiên từ đăng ký → trusted ngay
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

        // 4. Xử lý device theo risk level
        handleDeviceByRisk(risk, userDB, request, response, refreshTokenId);

        return ResponseEntity.ok(new SignUpResponse(accessToken));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFY DEVICE — Phase 2: xác thực OTP sau HIGH risk block
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public ResponseEntity<SignUpResponse> verifyDevice(HttpServletRequest request,
            HttpServletResponse response,
            VerifyDeviceRequest verifyDeviceRequest) throws Exception {
        // 1. Lấy pending context từ Redis
        String key = REDIS_PENDING_LOGIN + verifyDeviceRequest.verificationToken();
        PendingLoginCache pending = (PendingLoginCache) redisTemplate.opsForValue().get(key);
        if (pending == null) {
            throw new Exception("Mã xác thực không tồn tại hoặc đã hết hạn (10 phút).");
        }

        // 2. Kiểm tra OTP
        if (!pending.otp().equals(verifyDeviceRequest.otp())) {
            throw new IllegalArgumentException("OTP không đúng.");
        }

        // 3. Xoá key one-time
        redisTemplate.delete(key);

        // 4. Tìm lại user
        User userDB = userRepository.findByEmail(pending.email())
                .orElseThrow(() -> new Exception("User not found"));

        // 5. Tạo session đầy đủ
        UUID refreshTokenId = UUID.randomUUID();
        String accessToken = jwtService.generateToken(userDB.getEmail(), refreshTokenId);
        String refreshToken = jwtService.generateRefreshToken(userDB.getEmail(), refreshTokenId);
        long maxAgeSeconds = TimeUnit.DAYS.toSeconds(REFRESH_TTL_DAYS);

        setAuthCookies(response, refreshToken, maxAgeSeconds);
        redisTemplate.opsForValue().set(REDIS_REFRESH + refreshTokenId, "ACTIVE", maxAgeSeconds, TimeUnit.SECONDS);
        saveUserSession(userDB.getId(), refreshTokenId.toString(), maxAgeSeconds, request);

        // 6. Tạo UserDevice mới với isTrusted=true (đã xác thực OTP = tin cậy)
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
    // REPORT DEVICE — "Đây không phải là tôi"
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void reportDevice(String actionToken) throws Exception {
        String deviceId = (String) redisTemplate.opsForValue().get(REDIS_ACTION_TOKEN + actionToken);
        if (deviceId == null) {
            throw new Exception("Token không tồn tại hoặc đã hết hạn.");
        }
        redisTemplate.delete(REDIS_ACTION_TOKEN + actionToken);

        UserDevice device = userDeviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new Exception("Không tìm thấy thiết bị."));

        String jti = device.getSessionJti();
        Long userId = device.getUser().getId();
        if (jti != null) {
            redisTemplate.delete(REDIS_REFRESH + jti);
            redisTemplate.opsForHash().delete(REDIS_USER_SESSIONS + userId, jti);
        }
        // Đặt lại isTrusted=false → lần đăng nhập tiếp theo từ thiết bị này
        // sẽ bị đánh giá là MEDIUM (nếu IP quen) hoặc HIGH (nếu IP lạ) và gửi mail lại
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
     * Tính risk level dựa trên device token + subnet.
     * NONE : isTrusted=true + cùng subnet → vào thẳng
     * LOW : isTrusted=true + khác subnet → vào thẳng (reset modem OK)
     * MEDIUM : device lạ + cùng subnet → vào + warning email
     * HIGH : device lạ + khác subnet → CHẶN + OTP
     */
    private RiskLevel evaluateRisk(User userDB, HttpServletRequest request) {
        String deviceIdCookie = CookieUtil.read(request, "device_id");
        String currentIp = RequestUtils.getClientIp(request);

        // ─── Case: có device cookie ────────────────────────────────────────────
        if (deviceIdCookie != null) {
            UserDevice existing = userDeviceRepository
                    .findByUserIdAndDeviceId(userDB.getId(), deviceIdCookie)
                    .orElse(null);

            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsTrusted())) {
                    // NONE: device tin cậy + cùng subnet
                    // LOW : device tin cậy + IP đổi (reset modem)
                    return IpSubnetUtil.isSameSubnet(currentIp, existing.getLastIp())
                            ? RiskLevel.NONE
                            : RiskLevel.LOW;
                }
                // isTrusted=false → đã biết nhưng chưa tin (bị report trước đó)
                return IpSubnetUtil.isSameSubnet(currentIp, existing.getLastIp())
                        ? RiskLevel.MEDIUM
                        : RiskLevel.HIGH;
            }
            // Cookie có nhưng không tìm thấy trong DB → coi như thiết bị mới
        }

        // ─── Case: không có cookie (new device) hoặc cookie không hợp lệ ───────
        // So IP với TẤT CẢ các thiết bị đã biết của user
        // → Cùng subnet với bất kỳ device nào: MEDIUM (IP quen, device mới)
        // → Không khớp subnet nào: HIGH (IP lạ + device lạ)
        boolean ipKnown = userDeviceRepository.findAllByUserId(userDB.getId())
                .stream()
                .anyMatch(d -> IpSubnetUtil.isSameSubnet(currentIp, d.getLastIp()));

        return ipKnown ? RiskLevel.MEDIUM : RiskLevel.HIGH;
    }

    /**
     * NONE / LOW / MEDIUM: update hoặc tạo device.
     * HIGH không được gọi vào đây (đã throw trước ở signIn).
     */
    private void handleDeviceByRisk(RiskLevel risk, User userDB, HttpServletRequest request,
            HttpServletResponse response, UUID refreshTokenId) {
        String deviceIdCookie = CookieUtil.read(request, "device_id");
        String currentIp = RequestUtils.getClientIp(request);
        String currentUa = request.getHeader("User-Agent");
        long now = System.currentTimeMillis();

        // NONE & LOW: trusted device đã có — update silent
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

        // MEDIUM: device lạ + IP quen → tạo device mới isTrusted=true + warning email
        // IP quen → khả năng cao là user thật (đổi thiết bị / reinstall OS).
        // Tin tưởng ngay, nhưng vẫn cảnh báo để user biết.
        // Nếu không phải họ, click link "Không phải tôi" → revoke session +
        // isTrusted=false.
        String newDeviceId = UUID.randomUUID().toString();
        UserDevice newDevice = UserDevice.builder()
                .user(userDB)
                .deviceId(newDeviceId)
                .isTrusted(true) // trusted ngay vì IP quen
                .sessionJti(refreshTokenId.toString())
                .userAgent(currentUa)
                .lastActive(now)
                .lastIp(currentIp)
                .createdAt(now)
                .build();
        userDeviceRepository.save(newDevice);
        setDeviceIdCookie(response, newDeviceId, TimeUnit.DAYS.toSeconds(DEVICE_COOKIE_DAYS));

        // ActionToken 24h cho link "Đây không phải là tôi"
        String actionToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REDIS_ACTION_TOKEN + actionToken, newDeviceId, ACTION_TOKEN_HOURS, TimeUnit.HOURS);

        String reportLink = "http://localhost:8080/api/v1/auth/report-device?token=" + actionToken;
        String emailBody = """
                Hệ thống phát hiện đăng nhập từ thiết bị không tin cậy vào tài khoản của bạn.

                🖥  Thiết bị : %s
                🌐  Địa chỉ IP: %s
                ⚠️  Mức rủi ro: MEDIUM (IP quen nhưng thiết bị chưa tin cậy)

                Nếu ĐÂY KHÔNG PHẢI LÀ BẠN, nhấn link để thu hồi phiên đăng nhập ngay:
                👉 %s

                Link có hiệu lực trong 24 giờ.
                """.formatted(currentUa, currentIp, reportLink);

        emailService.sendEmail(
                userDB.getEmail(),
                "⚠️ Cảnh báo: Đăng nhập từ thiết bị chưa tin cậy",
                emailBody);
    }

    /**
     * HIGH risk: không tạo session, sinh OTP + verificationToken, gửi email, throw
     * exception.
     * Frontend nhận 403 + verificationToken → redirect sang màn nhập OTP.
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
                Có người đang cố đăng nhập vào tài khoản của bạn từ một thiết bị và IP hoàn toàn mới.

                🖥  Thiết bị : %s
                🌐  Địa chỉ IP: %s
                ⚠️  Mức rủi ro: HIGH

                Nếu ĐÂY LÀ BẠN, hãy nhập mã OTP sau vào ứng dụng:

                🔐  Mã OTP: %s

                Mã có hiệu lực trong 10 phút.
                Nếu không phải bạn, hãy bỏ qua email này. Hệ thống đã chặn đăng nhập này.
                """.formatted(currentUa, currentIp, otp);

        emailService.sendEmail(
                userDB.getEmail(),
                "🔐 Xác thực đăng nhập mới — Mã OTP của bạn",
                emailBody);

        // Throw → GlobalExceptionHandler trả 403 + verificationToken cho Frontend
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
