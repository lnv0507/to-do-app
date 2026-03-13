package vn.com.anhemsoftware.license_app.payload.auth.request;
import jakarta.validation.constraints.NotBlank;
public record SignInRequest(
        @NotBlank(message = "Email must not be blank") String email,
        @NotBlank(message = "Password must not be blank") String password)
{
}
