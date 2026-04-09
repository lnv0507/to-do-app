package vn.com.anhemsoftware.license_app.payload.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for AI prompt endpoint.
 */
public record AiPromptRequest(

                @NotBlank(message = "Prompt cannot be empty") @Size(max = 4000, message = "Prompt cannot exceed 4000 characters") String prompt) {
}
