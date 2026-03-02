package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.mapper.UserMapper;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
import vn.com.anhemsoftware.license_app.repository.UserRepository;
import vn.com.anhemsoftware.license_app.service.AuthService;
import vn.com.anhemsoftware.license_app.util.OptionalValidator;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService
{
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<User> signUp(SignUpRequest request) throws Exception
    {
        OptionalValidator.of(request , "Request must not be null")
                .requireNonBlank(SignUpRequest::email,"Email must not be blank")
                .requireNonBlank(SignUpRequest::fullName,"Full name must not be blank")
                .requireNonBlank(SignUpRequest::password,"Password must not be blank");

        User entity = userMapper.toUser(request);
        entity.setPassword(passwordEncoder.encode(request.password()));
        return ResponseEntity.accepted().body(userRepository.save(entity));
    }

    @Override
    public ResponseEntity<User> signIn(SignInRequest request) throws Exception
    {
        OptionalValidator.of(request , "Request must not be null")
                .requireNonBlank(SignInRequest::email,"Email must not be blank")
                .requireNonBlank(SignInRequest::password,"Password must not be blank");

        User entity = userMapper.toUser(request);
        return ResponseEntity.ok(userRepository.save(entity));
    }
}
