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
 * Extract lowest-level configuration here to easily swap models or add custom headers / 
 * proxy later.
 */
@Configuration
public class OpenAiConfig {

    /** API key retrieved from environment variable OPENAI_API_KEY */
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * Model name read from property {@code spring.ai.openai.model}.
     * Default {@code gpt-4o-mini} is declared in application.properties.
     * Override using env var {@code OPENAI_MODEL} when model change is needed.
     */
    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${spring.ai.openai.temperature:0.4}")
    private Double temperature;

    @Value("${spring.ai.openai.max-tokens:2000}")
    private Integer maxTokens;

    /**
     * Low-level HTTP client – responsible for sending/receiving HTTP to OpenAI REST API.
     * Inject this bean if you need custom headers, retry policy, or a proxy.
     */
    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Adapter layer – converts {@link org.springframework.ai.chat.prompt.Prompt}
     * from Spring AI to the format required by OpenAI REST API.
     * This is where you select the "brain" (model name) and hyperparameters.
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
