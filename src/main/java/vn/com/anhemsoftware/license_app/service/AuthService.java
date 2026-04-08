package vn.com.anhemsoftware.license_app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.com.anhemsoftware.license_app.payload.auth.request.ChangePasswordRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyDeviceRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;

public interface AuthService {
        void signUp(SignUpRequest request) throws Exception;

        ResponseEntity<SignUpResponse> confirmOtp(HttpServletRequest request, HttpServletResponse response,
                        VerifyOtpRequest verifyOtpRequest) throws Exception;

        ResponseEntity<SignUpResponse> signIn(HttpServletResponse response, SignInRequest signInRequest,
                        HttpServletRequest request) throws Exception;

        ResponseEntity<SignUpResponse> refreshToken(HttpServletRequest request, HttpServletResponse response)
                        throws Exception;

        void logoutAllExcept(Long userId, String currentJti) throws Exception;

        void changePassword(String email, ChangePasswordRequest request) throws Exception;

        void resetPassword(ResetPasswordRequest request) throws Exception;

        /**
         * Scenario HIGH: Xác thực OTP sau khi bị block.
         * Tạo session + UserDevice + cấp token nếu OTP đúng.
         */
        ResponseEntity<SignUpResponse> verifyDevice(HttpServletRequest request,
                        HttpServletResponse response,
                        VerifyDeviceRequest verifyDeviceRequest) throws Exception;
}
