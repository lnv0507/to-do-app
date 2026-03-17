package vn.com.anhemsoftware.license_app.payload.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body gửi lên khi user nhập OTP xác thực thiết bị (Scenario HIGH).
 *
 * @param verificationToken UUID ngắn hạn từ response 403 của signIn
 * @param otp               6 chữ số gửi qua email
 */
public record VerifyDeviceRequest(
        @NotBlank String verificationToken,
        @NotBlank @Size(min = 6, max = 6) String otp) {
}
