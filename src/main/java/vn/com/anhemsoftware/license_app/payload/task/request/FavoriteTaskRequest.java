package vn.com.anhemsoftware.license_app.payload.task.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteTaskRequest {

    @NotNull(message = "isFavorite must not be null")
    private Boolean isFavorite;
}
