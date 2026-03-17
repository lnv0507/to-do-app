package vn.com.anhemsoftware.license_app.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        /**
         * Được gọi khi user click "Đây không phải là tôi" trong email cảnh báo.
         * Revoke session của thiết bị lạ đó khỏi Redis.
         */
        void reportDevice(String actionToken) throws Exception;

        /**
         * Scenario HIGH: Xác thực OTP sau khi bị block.
         * Tạo session + UserDevice + cấp token nếu OTP đúng.
         */
        ResponseEntity<SignUpResponse> verifyDevice(HttpServletRequest request,
                        HttpServletResponse response,
                        VerifyDeviceRequest verifyDeviceRequest) throws Exception;
}
