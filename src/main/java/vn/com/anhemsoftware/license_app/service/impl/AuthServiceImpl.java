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
import vn.com.anhemsoftware.license_app.payload.auth.request.ChangePasswordRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.ResetPasswordRequest;
import vn.com.anhemsoftware.license_app.payload.auth.internal.GeoLocation;
import vn.com.anhemsoftware.license_app.service.GeoLocationService;
import vn.com.anhemsoftware.license_app.util.CookieUtil;
import vn.com.anhemsoftware.license_app.util.OTPGenerator;
import vn.com.anhemsoftware.license_app.util.RequestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // ─── Redis key prefixes ────────────────────────────────────────────────────
    @Value("${auth.redis.prefix.refresh:REFRESH:}")
    private String REDIS_REFRESH;

    @Value("${auth.redis.prefix.user-sessions:USER_SESSIONS:}")
    private String REDIS_USER_SESSIONS;

    @Value("${auth.redis.prefix.reset-password:RESET_PASS:}")
    private String REDIS_RESET_PASSWORD;

    /** Pending HIGH-risk login chờ OTP — TTL 10 phút */
    @Value("${auth.redis.prefix.pending-login:PENDING_LOGIN:}")
    private String REDIS_PENDING_LOGIN;

    // ─── TTL constants ─────────────────────────────────────────────────────────
    @Value("${auth.ttl.refresh-days:7}")
    private long REFRESH_TTL_DAYS;

    @Value("${auth.ttl.device-cookie-days:1825}")
    private long DEVICE_COOKIE_DAYS;

    @Value("${auth.ttl.pending-login-minutes:10}")
    private long PENDING_LOGIN_MINUTES;

    // ─── Dependencies ──────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserDeviceRepository userDeviceRepository;
    private final GeoLocationService geoLocationService;

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
    // CHANGE PASSWORD & RESET PASSWORD
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public void changePassword(String email, ChangePasswordRequest request) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu cũ không chính xác.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Lấy jti hiện tại từ context (có thể pass từ JWT lọc trên Authentication, nhưng ở đây ta có thể dùng ContextHolder.
        // Tạm thời để đơn giản, gọi reset tất cả phiên ngoại trừ hiện tại (nếu có context jti).
        // Nếu không có jti hiện tại, log out TẤT CẢ (Global Revoke).
        logoutAllExcept(user.getId(), "DUMMY_JTI_TO_LOGOUT_ALL");

        emailService.sendEmail(user.getEmail(), "Mật khẩu Cập nhật Thành công", "Mật khẩu của bạn vừa được thay đổi. Nếu không phải bạn, hãy liên hệ hỗ trợ ngay.");
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) throws Exception {
        String email = (String) redisTemplate.opsForValue().get(REDIS_RESET_PASSWORD + request.token());
        if (email == null) {
            throw new Exception("Token đổi mật khẩu không hợp lệ hoặc đã hết hạn.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User not found"));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        redisTemplate.delete(REDIS_RESET_PASSWORD + request.token());

        // Global Revoke tất cả thiết bị
        logoutAllExcept(user.getId(), "DUMMY_JTI_TO_LOGOUT_ALL");

        emailService.sendEmail(user.getEmail(), "Mật khẩu Cập nhật Thành công", "Mật khẩu của bạn đã được đặt lại thành công sau khi báo cáo rủi ro.");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RBA ENGINE
    // ═══════════════════════════════════════════════════════════════════════════

    private enum RiskLevel {
        NONE, LOW, MEDIUM, HIGH
    }

    /**
     * Tính risk level dựa trên Device cookie và GeoLocation (Vị trí).
     * Ma trận rủi ro:
     * - Cũ (Tin cậy) + Vị trí Quen: NONE / LOW
     * - Cũ (Tin cậy) + Vị trí Mới (Impossible travel): CRITICAL (tương đương HIGH nhưng lý do khác)
     * - Mới (Không cookie) + Vị trí Quen: MEDIUM (Mua máy mới, dùng 4G)
     * - Mới (Không cookie) + Vị trí Khác: HIGH (Bị lộ pass, hacker ở xa)
     */
    private RiskLevel evaluateRisk(User userDB, HttpServletRequest request) {
        String deviceIdCookie = CookieUtil.read(request, "device_id");
        String currentIp = RequestUtils.getClientIp(request);
        GeoLocation currentGeo = geoLocationService.getLocation(currentIp);

        // ─── Case: có device cookie (Thiết bị cũ) ──────────────────────────────
        if (deviceIdCookie != null) {
            UserDevice existing = userDeviceRepository
                    .findByUserIdAndDeviceId(userDB.getId(), deviceIdCookie)
                    .orElse(null);

            if (existing != null) {
                GeoLocation existingGeo = geoLocationService.getLocation(existing.getLastIp());
                boolean isSameLocation = currentGeo.isSameLocation(existingGeo);

                if (Boolean.TRUE.equals(existing.getIsTrusted())) {
                    // Cũ + Quen
                    if (isSameLocation) return RiskLevel.NONE;
                    // Cũ + Lạ = CRITICAL (Impossible travel có thể xảy ra)
                    return RiskLevel.HIGH; 
                }
                // isTrusted=false → Cookie bị report xấu
                return isSameLocation ? RiskLevel.MEDIUM : RiskLevel.HIGH;
            }
        }

        // ─── Case: không có cookie (Thiết bị mới) ─────────────────────────────
        // So vị trí với một trong các thiết bị đã biết của user
        boolean locationKnown = userDeviceRepository.findAllByUserId(userDB.getId())
                .stream()
                .anyMatch(d -> {
                    GeoLocation savedGeo = geoLocationService.getLocation(d.getLastIp());
                    return currentGeo.isSameLocation(savedGeo);
                });

        return locationKnown ? RiskLevel.MEDIUM : RiskLevel.HIGH;
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

        // Token 2h cho link đổi mật khẩu
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                REDIS_RESET_PASSWORD + resetToken, userDB.getEmail(), 2, TimeUnit.HOURS);

        String resetLink = "http://localhost:3000/reset-password?token=" + resetToken;
        String emailBody = """
                Hệ thống phát hiện đăng nhập từ thiết bị mới.

                🖥  Thiết bị : %s
                🌐  Địa chỉ IP: %s

                Nếu ĐÂY LÀ BẠN, hãy phớt lờ email này.
                Nếu ĐÂY KHÔNG PHẢI LÀ BẠN, vui lòng BẤM VÀO ĐÂY ĐỂ ĐỔI MẬT KHẨU ngay lập tức. Sau khi đổi mật khẩu, hệ thống sẽ đăng xuất tài khoản của bạn khỏi tất cả các thiết bị.
                👉 %s

                Link có hiệu lực trong 2 giờ.
                """.formatted(currentUa, currentIp, resetLink);

        emailService.sendEmail(
                userDB.getEmail(),
                "Cảnh báo bảo mật: Thay đổi mật khẩu ngay nếu đây không phải bạn",
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
