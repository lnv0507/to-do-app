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
        // Prefix for messages sent from server to client
        config.enableSimpleBroker("/topic");
        // Prefix for messages sent from client to server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint for clients to connect to WebSocket
        registry.addEndpoint("/api/v1/ws")
                .setAllowedOriginPatterns("https://app.practicehandler.io.vn", "http://localhost:3000") // Enable CORS
                .withSockJS(); // Fallback for browsers not supporting WebSocket
    }
}
