package vn.com.anhemsoftware.license_app.payload.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse
{
    String title;
    String details;
    int status;
    String instance;
    LocalDateTime timestamp;

    public static ApiErrorResponse notFound(String title, String details, String instance)
    {
        return ApiErrorResponse.builder()
                .title(title)
                .details(details)
                .instance(instance)
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiErrorResponse clientError(String title, String details, String instance)
    {
        return ApiErrorResponse.builder()
                .title(title)
                .details(details)
                .instance(instance)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ApiErrorResponse serverError(String title, String details, String instance)
    {
        return ApiErrorResponse.builder()
                .title(title)
                .details(details)
                .instance(instance)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
