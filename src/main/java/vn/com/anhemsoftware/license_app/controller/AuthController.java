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
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
import vn.com.anhemsoftware.license_app.service.AuthService;

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

    /**
     * "This is not me" — link in MEDIUM risk email.
     * No auth required — ActionToken is proof.
     */
    @GetMapping("/report-device")
    public ResponseEntity<String> reportDevice(@RequestParam String token) throws Exception {
        authService.reportDevice(token);
        return ResponseEntity.ok(
                "✅ Login session from an unknown device has been revoked. " +
                        "If you are concerned, please change your password immediately.");
    }
}
