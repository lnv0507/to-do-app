package vn.com.anhemsoftware.license_app.payload.auth.request;

public record AuthRequest(
        String email,
        String password
) {

}
