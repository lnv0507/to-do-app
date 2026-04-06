package vn.com.anhemsoftware.license_app.config;

import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Layer 1 & 2 – Low-level Spring AI configuration.
 *
 * <pre>
 * OpenAiApi  →  OpenAiChatModel  →  ChatClient  →  AiInferenceService
 * (HTTP)        (Adapter)           (Fluent API)    (Business logic)
 * </pre>
 *
 * Tách cấu hình thấp nhất ra đây để dễ swap model hoặc thêm custom headers /
 * proxy sau này.
 */
@Configuration
public class OpenAiConfig {

    /** API key lấy từ environment variable OPENAI_API_KEY */
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * Tên model đọc từ property {@code spring.ai.openai.model}.
     * Default {@code gpt-4o-mini} được khai báo trong application.properties.
     * Override bằng env var {@code OPENAI_MODEL} khi cần thay model.
     */
    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${spring.ai.openai.temperature:0.4}")
    private Double temperature;

    @Value("${spring.ai.openai.max-tokens:2000}")
    private Integer maxTokens;

    /**
     * Low-level HTTP client – chịu trách nhiệm gửi/nhận HTTP tới OpenAI REST API.
     * Inject bean này nếu bạn cần custom headers, retry policy hoặc dùng proxy.
     */
    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Adapter layer – chuyển đổi {@link org.springframework.ai.chat.prompt.Prompt}
     * của Spring AI thành format mà OpenAI REST API yêu cầu.
     * Đây là nơi bạn chọn "não bộ" (model name) và các hyperparameter.
     */
    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                // .temperature(temperature)
                // .maxTokens(maxTokens)
                .reasoningEffort("medium")
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
