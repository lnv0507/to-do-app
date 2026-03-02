package vn.com.anhemsoftware.license_app.service;

import org.springframework.http.ResponseEntity;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;

public interface AuthService
{
    ResponseEntity<SignUpResponse> signUp(SignUpRequest request) throws Exception;
    ResponseEntity<User> signIn(SignInRequest request) throws Exception;
}
