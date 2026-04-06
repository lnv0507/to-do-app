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
 * REST API cho Spring AI inference.
 *
 * <p>Endpoints:
 * <ul>
 * <li>{@code POST /api/v1/ai/ask} — gửi prompt, nhận response từ model (yêu cầu
 * JWT)</li>
 * </ul>
 *
 * <p>Chat memory: mỗi user có một {@code conversationId} riêng ({@code
 * "user-{id}"}).
 * ID này được lấy từ JWT token đã xác thực — client không thể giả mạo.
 *
 * <p>Ví dụ request:
 * <pre>{@code
 * POST /api/v1/ai/ask
 * Authorization: Bearer <token>
 * Content-Type: application/json
 *
 * {
 * "prompt": "Giải thích Spring AI trong 3 dòng"
 * }
 * }</pre>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiInferenceService aiInferenceService;

    /**
     * Gửi prompt tới model và trả về response, kèm lịch sử hội thoại của user.
     *
     * @param request body chứa {@code prompt} (bắt buộc)
     * @param auth    Spring Security Authentication — inject tự động, luôn có vì
     *                endpoint yêu cầu JWT
     * @return {@link AiPromptResponse} chứa nội dung phản hồi từ model
     */
    @PostMapping("/ask")
    public ResponseEntity<AiPromptResponse> ask(
            @Valid @RequestBody AiPromptRequest request,
            Authentication auth) {

        // Lấy userId từ JWT token đã xác thực — không nhận từ client để chống giả mạo
        User user = (User) auth.getPrincipal();
        String conversationId = "user-" + user.getId();

        String result = aiInferenceService.ask(request.prompt(), conversationId);

        return ResponseEntity.ok(new AiPromptResponse(result));
    }
}
