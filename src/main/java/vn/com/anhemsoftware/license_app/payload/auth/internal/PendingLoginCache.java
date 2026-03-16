package vn.com.anhemsoftware.license_app.payload.auth.internal;

/**
 * Dữ liệu login đang chờ xác thực OTP (Scenario HIGH).
 * Lưu trong Redis với key "PENDING_LOGIN:<verificationToken>", TTL 10 phút.
 */
public record PendingLoginCache(
        String email,
        String otp,
        String ip,
        String userAgent) {
}
