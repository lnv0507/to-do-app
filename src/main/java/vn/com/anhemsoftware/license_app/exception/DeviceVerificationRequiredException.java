package vn.com.anhemsoftware.license_app.exception;

import lombok.Getter;

/**
 * Ném ra khi login từ thiết bị HIGH risk.
 * Chứa verificationToken để frontend redirect sang màn nhập OTP.
 * HTTP Status: 403 (xử lý trong GlobalExceptionHandler).
 */
@Getter
public class DeviceVerificationRequiredException extends RuntimeException {
    private final String verificationToken;

    public DeviceVerificationRequiredException(String verificationToken) {
        super("REQUIRE_VERIFICATION");
        this.verificationToken = verificationToken;
    }
}
