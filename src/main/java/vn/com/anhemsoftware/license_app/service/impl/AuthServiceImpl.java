package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.mapper.UserMapper;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.VerifyOtpRequest;
import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
import vn.com.anhemsoftware.license_app.repository.UserRepository;
import vn.com.anhemsoftware.license_app.service.AuthService;
import vn.com.anhemsoftware.license_app.service.JWTService;
import vn.com.anhemsoftware.license_app.util.OTPGenerator;
import vn.com.anhemsoftware.license_app.util.OptionalValidator;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final RedisTemplate redisTemplate;

    @Override
    public void signUp(SignUpRequest request) throws Exception {
        OptionalValidator.of(request, "Request must not be null")
                .requireNonBlank(SignUpRequest::email, "Email must not be blank")
                .requireNonBlank(SignUpRequest::fullName, "Full name must not be blank")
                .requireNonBlank(SignUpRequest::password, "Password must not be blank");
        // check exist email
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new Exception("Email already exists");
        }
//        handle OTP
        String otp = OTPGenerator.generateOTP();
        HashMap<String, Object> otpMap = new HashMap<>();
        otpMap.put("request", request);
        otpMap.put("otp", otp);

        // storage in redis
        redisTemplate.opsForValue().set("REG:" + request.email(), otpMap, 5, TimeUnit.MINUTES);

    }

    @Override
    public ResponseEntity<SignUpResponse> confirmOtp(VerifyOtpRequest request) throws Exception {
        OptionalValidator.of(request, "Request must not be null")
                .requireNonBlank(VerifyOtpRequest::email, "Email must not be blank")
                .requireNonBlank(VerifyOtpRequest::otp, "OTP must not be blank");
        // extract otp and request from redis
        //handle null
        if (redisTemplate.opsForValue().get("REG:" + request.email()) == null) {
            throw new Exception("OTP is not correct");
        }
        //handle wrong otp
        HashMap<String, Object> otpMap = (HashMap<String, Object>) redisTemplate.opsForValue().get("REG:" + request.email());
        String otp = otpMap.get("otp").toString();
        SignUpRequest signUpRequest = (SignUpRequest) otpMap.get("request");
        if (!otp.equals(request.otp())) {
            throw new Exception("OTP is not correct");
        }
        //clear redis
        redisTemplate.delete("REG:" + request.email());
        // save user
        User userEntity = userMapper.toUser(signUpRequest);
        userEntity.setPassword(passwordEncoder.encode(signUpRequest.password()));
        User userDB = userRepository.save(userEntity);
        String accessToken = jwtService.generateToken(userDB.getEmail());
        String refreshToken = jwtService.generateRefreshToken(userDB.getEmail());
        return ResponseEntity.ok(new SignUpResponse(accessToken, refreshToken));


    }

    @Override
    public ResponseEntity<SignUpResponse> signIn(SignInRequest request) throws Exception {
        OptionalValidator.of(request, "Request must not be null")
                .requireNonBlank(SignInRequest::email, "Email must not be blank")
                .requireNonBlank(SignInRequest::password, "Password must not be blank");

        User entity = userMapper.toUser(request);
        // user not found
        User userDB = userRepository.findByEmail(request.email()).orElseThrow(() -> new Exception("User not found"));
        
        // password not match
        if (!passwordEncoder.matches(request.password(), userDB.getPassword())) {
            throw new Exception("Password not match");
        }

        String accessToken = jwtService.generateToken(entity.getEmail());
        String refreshToken = jwtService.generateRefreshToken(entity.getEmail());
        return ResponseEntity.ok(new SignUpResponse(accessToken, refreshToken));

    }
}
