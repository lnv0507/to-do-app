package vn.com.anhemsoftware.license_app.payload.auth.request;

import lombok.With;

@With
public record SignUpRequest(
        String email,
        String password,
        String fullName,
        String phone,
        String address)
{
}
