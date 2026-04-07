package vn.com.anhemsoftware.license_app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix cho message từ server gửi đến client
        config.enableSimpleBroker("/topic");
        // Prefix cho message từ client gửi đến server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client connect vào WebSocket
        registry.addEndpoint("/api/v1/ws")
                .setAllowedOriginPatterns("https://app.practicehandler.io.vn", "http://localhost:3000") // Cho phép CORS
                                                                                                        // từ mọi nguồn
                                                                                                        // (có thể
                // điều chỉnh)
                .withSockJS(); // Fallback cho browser không support WebSocket
    }
}
