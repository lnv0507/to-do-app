package vn.com.anhemsoftware.license_app.payload.auth.internal;

import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;

public record OtpCacheDto (SignUpRequest signUpRequest, String otp) {

}
