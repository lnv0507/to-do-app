package vn.com.anhemsoftware.license_app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;

public interface AuthService
{
    void signUp(SignUpRequest request) throws Exception;
    ResponseEntity<SignUpResponse> confirmOtp(VerifyOtpRequest request) throws Exception;
    ResponseEntity<SignUpResponse> signIn(SignInRequest request) throws Exception;
}
