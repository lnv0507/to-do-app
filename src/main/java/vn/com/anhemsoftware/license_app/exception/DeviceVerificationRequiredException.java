package vn.com.anhemsoftware.license_app.exception;

import lombok.Getter;

/**
 * Thrown when logging in from a HIGH risk device.
 * Contains verificationToken for frontend to redirect to OTP input screen.
 * HTTP Status: 403 (handled in GlobalExceptionHandler).
 */
@Getter
public class DeviceVerificationRequiredException extends RuntimeException {
    private final String verificationToken;

    public DeviceVerificationRequiredException(String verificationToken) {
        super("REQUIRE_VERIFICATION");
        this.verificationToken = verificationToken;
    }
}
