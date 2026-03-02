package vn.com.anhemsoftware.license_app.payload.auth.request;
import lombok.With;
@With
public record VerifyOtpRequest(
        String email,
        String otp)
{
    
}