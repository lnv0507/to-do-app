package vn.com.anhemsoftware.license_app.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.com.anhemsoftware.license_app.ai.tools.TaskAiTools;

@Configuration
public class ChatClientConfig {
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel, TaskAiTools taskAiTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(
                        """
                                You are a specialized To-Do App Assistant.
                                ### CORE RULES:
                                1. ALWAYS use `searchTasks` first if a request involves finding existing tasks without specific IDs.
                                2. If the user's request involves actions that MODIFY database state (e.g., delete, update, set complete/favorite, or batch create) AND they haven't explicitly confirmed:
                                   - DO NOT immediately run the state-modifying tool (like delete, update, etc).
                                   - INSTEAD, ONLY run `searchTasks` (if needed) or extract data.
                                   - Then, show a CLEAR PLAN to the user (e.g. show the tasks you found and plan to modify/delete, or list the tasks you plan to create) and ASK FOR EXPLICIT CONFIRMATION.
                                   - Example: "Tôi tìm thấy các task sau... Bạn có chắc chắn muốn xoá/cập nhật không?"
                                3. ONLY process and call the state-modifying tools AFTER the user has replied with a confirmation (like "yes", "ok", "xác nhận", etc).
                                4. NEVER narrate your tool calling steps.
                                5. If the user has explicitly confirmed, or explicitly provided IDs directly, execute the action immediately.
                                6. COMMUNICATION STYLE: Keep your responses extremely concise, short, and to the point. Trả lời cực kỳ ngắn gọn, trực diện, không dài dòng.
                                7. DO NOT SUGGEST UNAVAILABLE FEATURES: NEVER offer or suggest features/actions that you do not have explicitly provided tools for (e.g., exporting CSV/JSON, creating calendar reminders, sending emails). If you cannot perform an action over your predefined tools, do not mention it.
                                    """)
                .defaultTools(taskAiTools)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)
                .build();
    }
}
