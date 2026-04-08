package vn.com.anhemsoftware.license_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyDeviceRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.ChangePasswordRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.ResetPasswordRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
import vn.com.anhemsoftware.license_app.service.AuthService;
import java.security.Principal;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignUpRequest request) throws Exception {
        authService.signUp(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/confirm-otp")
    public ResponseEntity<SignUpResponse> confirmOtp(HttpServletRequest request, HttpServletResponse response,
            @RequestBody VerifyOtpRequest verifyOtpRequest) throws Exception {
        return authService.confirmOtp(request, response, verifyOtpRequest);
    }

    @PostMapping("/signin")
    public ResponseEntity<SignUpResponse> signin(HttpServletRequest request, HttpServletResponse response,
            @RequestBody SignInRequest signInRequest)
            throws Exception {
        return authService.signIn(response, signInRequest, request);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<SignUpResponse> refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        return authService.refreshToken(request, response);
    }

    /**
     * Phase 2 — OTP Authentication after HIGH risk block.
     * Frontend receives 403 from /signin (with verificationToken) → calls this
     * endpoint.
     */
    @PostMapping("/verify-device")
    public ResponseEntity<SignUpResponse> verifyDevice(HttpServletRequest request, HttpServletResponse response,
            @Valid @RequestBody VerifyDeviceRequest verifyDeviceRequest) throws Exception {
        return authService.verifyDevice(request, response, verifyDeviceRequest);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) throws Exception {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập.");
        }
        authService.changePassword(principal.getName(), request);
        return ResponseEntity.ok("✅ Mật khẩu thay đổi thành công. Bạn đã được đăng xuất an toàn khỏi các thiết bị khác.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) throws Exception {
        authService.resetPassword(request);
        return ResponseEntity.ok("✅ Mật khẩu thiết lập lại thành công. Bạn đã được bảo mật an toàn trên tất cả các nền tảng.");
    }
}
