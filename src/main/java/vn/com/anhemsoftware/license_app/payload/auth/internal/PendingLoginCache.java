package vn.com.anhemsoftware.license_app.payload.auth.internal;

/**
 * Login data pending OTP verification (Scenario HIGH).
 * Stored in Redis with key "PENDING_LOGIN:<verificationToken>", TTL 10 minutes.
 */
public record PendingLoginCache(
                String email,
                String otp,
                String ip,
                String userAgent) {
}
