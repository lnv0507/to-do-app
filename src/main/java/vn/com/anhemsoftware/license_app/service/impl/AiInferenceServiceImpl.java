package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import vn.com.anhemsoftware.license_app.service.AiInferenceService;

/**
 * Implementation of {@link AiInferenceService} using Spring AI
 * {@link ChatClient}.
 *
 * <p>
 * Fluent API pipeline:
 * 
 * <pre>
 * chatClient.prompt()   → initialize request builder
 *   .user(message)       → set user message
 *   .advisors(...)       → attach PromptChatMemoryAdvisor with user conversationId
 *   .call()              → send to model (blocking)
 *   .content()           → extract text content from response
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInferenceServiceImpl implements AiInferenceService {

        private final ChatClient chatClient;
        private final ChatMemory chatMemory;

        // -------------------------------------------------------------------------
        // Stateless — no chat memory (backward-compatible)
        // -------------------------------------------------------------------------

        /**
         * Send user message using the default system prompt set in
         * {@code ChatClientConfig}.
         * No chat memory — each call is an independent conversation.
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
        // Stateful — with chat memory (per-user, JDBC-backed)
        // -------------------------------------------------------------------------

        /**
         * Send user message with conversation history from MySQL.
         * {@code conversationId} is used to distinguish users — typically
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
