package vn.com.anhemsoftware.license_app.payload.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.With;

@With
public record VerifyOtpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "OTP must not be blank")
        String otp
) {

}
