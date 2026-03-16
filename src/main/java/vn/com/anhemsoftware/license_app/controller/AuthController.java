package vn.com.anhemsoftware.license_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
import vn.com.anhemsoftware.license_app.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
// Just for demo. Need to modify
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignUpRequest request) throws Exception {
        authService.signUp(request);
        return ResponseEntity.ok("User registered successfully");
    }
    @PostMapping("/confirm-otp")
    public ResponseEntity<SignUpResponse> confirmOtp(@RequestBody VerifyOtpRequest request) throws Exception {
        return authService.confirmOtp(request);
    }

    @PostMapping("/signin")
    public ResponseEntity<SignUpResponse> signin(@RequestBody SignInRequest request) throws Exception {
        return authService.signIn(request);
    }
}
