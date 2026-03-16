package vn.com.anhemsoftware.license_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.com.anhemsoftware.license_app.payload.auth.ApiErrorResponse;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
public class GlobalExceptionHandler {
    private final int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    private final int INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private final int CLIENT_ERROR = HttpStatus.BAD_REQUEST.value();

    /**
     * HIGH risk login — trả về 403 + verificationToken để Frontend redirect sang
     * màn OTP.
     */
    @ExceptionHandler(DeviceVerificationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceVerificationRequired(
            DeviceVerificationRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "REQUIRE_VERIFICATION");
        body.put("message", "Đăng nhập từ thiết bị lạ. Vui lòng xác thực qua mã OTP đã gửi đến email.");
        body.put("verificationToken", ex.getVerificationToken());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        String instanceId = request.getRequestURI();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.clientError("Validation Failed", details, instanceId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
            HttpServletRequest request) {
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex,
            HttpServletRequest request) {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(CLIENT_ERROR).body(ApiErrorResponse.clientError(title, details, instanceId));
    }


    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFoundException(IllegalArgumentException ex,
            HttpServletRequest request) {
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFoundException(IllegalArgumentException ex,
            HttpServletRequest request) {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(NOT_FOUND).body(ApiErrorResponse.notFound(title, details, instanceId));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTaskNotFoundException(TaskNotFoundException ex,
            HttpServletRequest request) {
        String title = "Không tìm thấy công việc (Task)";
        String detail = ex.getMessage();
        String instanceId = request.getRequestURI();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.notFound(title, detail, instanceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String title = "Lỗi xác thực dữ liệu";
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(CLIENT_ERROR)
                .body(ApiErrorResponse.clientError(title, details, instanceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUncheckedException(Exception ex, HttpServletRequest request) {
    public ResponseEntity<ApiErrorResponse> handleUncheckedException(Exception ex, HttpServletRequest request) {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.serverError(title, details, instanceId));
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.serverError(title, details, instanceId));
    }

}
