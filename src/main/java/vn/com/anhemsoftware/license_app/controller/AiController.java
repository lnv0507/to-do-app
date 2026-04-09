package vn.com.anhemsoftware.license_app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.com.anhemsoftware.license_app.entity.User;
import vn.com.anhemsoftware.license_app.payload.ai.AiPromptRequest;
import vn.com.anhemsoftware.license_app.payload.ai.AiPromptResponse;
import vn.com.anhemsoftware.license_app.service.AiInferenceService;

/**
 * REST API for Spring AI inference.
 *
 * <p>Endpoints:
 * <ul>
 * <li>{@code POST /api/v1/ai/ask} — send prompt, receive response from model (requires
 * JWT)</li>
 * </ul>
 *
 * <p>Chat memory: each user has a unique {@code conversationId} ({@code
 * "user-{id}"}).
 * This ID is retrieved from the authenticated JWT token — client cannot forge it.
 *
 * <p>Example request:
 * <pre>{@code
 * POST /api/v1/ai/ask
 * Authorization: Bearer <token>
 * Content-Type: application/json
 *
 * {
 * "prompt": "Explain Spring AI in 3 lines"
 * }
 * }</pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiInferenceService aiInferenceService;

    /**
     * Send prompt to model and return response, including user's chat history.
     *
     * @param request body containing {@code prompt} (mandatory)
     * @param auth    Spring Security Authentication — automatically injected, always present since
     *                endpoint requires JWT
     * @return {@link AiPromptResponse} containing response content from the model
     */
    @PostMapping("/ask")
    public ResponseEntity<AiPromptResponse> ask(
            @Valid @RequestBody AiPromptRequest request,
            Authentication auth) {

        // Get userId from authenticated JWT token — not received from client to prevent forgery
        User user = (User) auth.getPrincipal();
        String conversationId = "user-" + user.getId();

        String result = aiInferenceService.ask(request.prompt(), conversationId);

        return ResponseEntity.ok(new AiPromptResponse(result));
    }
}
