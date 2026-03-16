// package vn.com.anhemsoftware.license_app.Anhemsoftware_license_app.service;

// import jakarta.servlet.http.HttpServletResponse;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.core.ValueOperations;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import vn.com.anhemsoftware.license_app.entity.User;
// import vn.com.anhemsoftware.license_app.mapper.UserMapper;
// import vn.com.anhemsoftware.license_app.payload.auth.request.SignInRequest;
// import vn.com.anhemsoftware.license_app.payload.auth.request.SignUpRequest;
// import vn.com.anhemsoftware.license_app.payload.auth.response.SignUpResponse;
// import vn.com.anhemsoftware.license_app.repository.UserRepository;
// import vn.com.anhemsoftware.license_app.service.EmailService;
// import vn.com.anhemsoftware.license_app.service.JWTService;
// import vn.com.anhemsoftware.license_app.service.impl.AuthServiceImpl;

// import java.util.Optional;
// import java.util.UUID;
// import java.util.concurrent.TimeUnit;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class AuthServiceTest {
// @Mock
// private UserRepository userRepository;
// @Mock
// private RedisTemplate<String, Object> redisTemplate;
// @Mock
// private ValueOperations<String, Object> valueOperations; //for testing Redis
// @Mock
// private PasswordEncoder passwordEncoder;
// @Mock
// private JWTService jwtService;
// @Mock
// private EmailService emailService;
// @Mock
// private UserMapper userMapper;

// @InjectMocks
// private AuthServiceImpl authService;

// @BeforeEach
// void setUp() {
// // Connect ValueOperations to RedisTemplate
// lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
// }

// @Test
// @DisplayName("Đăng nhập thành công - Trả về Token")
// void signIn_Success() throws Exception {
// // 1. Arrange
// SignInRequest request = new SignInRequest("test@fsoft.com.vn", "123456");
// User mockUser = new User();
// mockUser.setEmail(request.email());
// mockUser.setPassword("encoded_password");

// HttpServletResponse response = mock(HttpServletResponse.class);

// when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(mockUser));
// when(passwordEncoder.matches(request.password(),
// mockUser.getPassword())).thenReturn(true);
// when(jwtService.generateToken(anyString(),
// any(UUID.class))).thenReturn("access_token");
// when(jwtService.generateRefreshToken(anyString(),
// any(UUID.class))).thenReturn("refresh_token");
// // 2. Act
// ResponseEntity<SignUpResponse> result = authService.signIn(response,
// request);

// // 3. Assert
// assertNotNull(result.getBody());
// assertEquals("access_token", result.getBody().accessToken());
// verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(),
// any(TimeUnit.class));
// verify(jwtService, times(1)).generateRefreshToken(anyString(),
// any(UUID.class));
// }

// @Test
// @DisplayName("Đăng ký thất bại - Email đã tồn tại")
// void signUp_Fail_EmailExists() {
// // arrange
// SignUpRequest request = new SignUpRequest("exist@gmail.com", "123", "Tuấn",
// "123", "123");

// when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new
// User()));

// // act
// Exception exception = assertThrows(Exception.class, () -> {
// authService.signUp(request);
// });

// assertEquals("Email already exists", exception.getMessage());
// verify(emailService, times(0)).sendEmail(anyString(), anyString(),
// anyString());
// }

// }
