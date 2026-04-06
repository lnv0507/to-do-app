package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import vn.com.anhemsoftware.license_app.service.AiInferenceService;

/**
 * Implementation của {@link AiInferenceService} sử dụng Spring AI
 * {@link ChatClient}.
 *
 * <p>
 * Fluent API pipeline:
 * 
 * <pre>
 * chatClient.prompt()   → khởi tạo request builder
 *   .user(message)       → set user message
 *   .advisors(...)       → gắn PromptChatMemoryAdvisor với conversationId của user
 *   .call()              → gửi tới model (blocking)
 *   .content()           → extract text content từ response
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInferenceServiceImpl implements AiInferenceService {

        private final ChatClient chatClient;
        private final ChatMemory chatMemory;

        // -------------------------------------------------------------------------
        // Stateless — không có chat memory (backward-compatible)
        // -------------------------------------------------------------------------

        /**
         * Gửi user message, dùng default system prompt đã set trong
         * {@code ChatClientConfig}.
         * Không có chat memory — mỗi lần gọi là một cuộc hội thoại độc lập.
         */
        @Override
        public String ask(String userMessage) {
                log.debug("[AI] Stateless prompt: {}", userMessage);

                String response = chatClient.prompt()
                                .user(userMessage)
                                .call()
                                .content();

                log.debug("[AI] Response: {}", response);
                return response;
        }

        // -------------------------------------------------------------------------
        // Stateful — có chat memory (per-user, JDBC-backed)
        // -------------------------------------------------------------------------

        /**
         * Gửi user message kèm lịch sử hội thoại từ MySQL.
         * {@code conversationId} dùng để phân biệt user — thường là
         * {@code "user-{userId}"}.
         */
        @Override
        public String ask(String userMessage, String conversationId) {
                log.debug("[AI] Memory prompt | conversationId={} | user: {}", conversationId, userMessage);

                String response = chatClient.prompt()
                                .user(userMessage)
                                .advisors(PromptChatMemoryAdvisor.builder(chatMemory)
                                                .conversationId(conversationId)
                                                .build())
                                .call()
                                .content();

                log.debug("[AI] Response | conversationId={}: {}", conversationId, response);
                return response;
        }

}
