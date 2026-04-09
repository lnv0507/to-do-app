package vn.com.anhemsoftware.license_app.payload.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body sent when user enters OTP for device verification (Scenario HIGH).
 *
 * @param verificationToken Short-lived UUID from signIn 403 response
 * @param otp               6-digit code sent via email
 */
public record VerifyDeviceRequest(
        @NotBlank String verificationToken,
        @NotBlank @Size(min = 6, max = 6) String otp) {
}
