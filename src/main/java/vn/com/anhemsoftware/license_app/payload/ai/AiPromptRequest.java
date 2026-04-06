package vn.com.anhemsoftware.license_app.payload.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body cho AI prompt endpoint.
 */
public record AiPromptRequest(

                @NotBlank(message = "Prompt không được để trống") @Size(max = 4000, message = "Prompt không được vượt quá 4000 ký tự") String prompt) {
}
