package vn.com.anhemsoftware.license_app.payload.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.With;


@With
public record SignUpRequest(
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 6, message = "Password must be at least 6 characters long")
        String password,

        @NotBlank(message = "Full name must not be blank")
        String fullName,

        @NotBlank(message = "Phone number must not be blank")
        String phone,

        @NotBlank(message = "Address must not be blank")
        String address
) {
}