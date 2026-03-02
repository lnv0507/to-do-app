package vn.com.anhemsoftware.license_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.com.anhemsoftware.license_app.payload.auth.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler
{
    private final int NOT_FOUND = HttpStatus.NOT_FOUND.value();
    private final int INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR.value();
    private final int CLIENT_ERROR = HttpStatus.BAD_REQUEST.value();

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request)
    {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(CLIENT_ERROR).body(ApiErrorResponse.clientError(title, details, instanceId));
    }
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFoundException(IllegalArgumentException ex, HttpServletRequest request)
    {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(NOT_FOUND).body(ApiErrorResponse.notFound(title, details, instanceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUncheckedException(Exception ex, HttpServletRequest request)
    {
        String title = ex.getLocalizedMessage();
        String details = ex.getMessage();
        String instanceId = request.getRequestURI();
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ApiErrorResponse.serverError(title, details, instanceId));
    }

}
