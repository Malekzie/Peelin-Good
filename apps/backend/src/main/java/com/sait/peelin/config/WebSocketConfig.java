package com.sait.peelin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.security.JwtHandshakeInterceptor;
import com.sait.peelin.service.ChatService;
import com.sait.peelin.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:4173,http://localhost:5173,http://localhost:8080,http://10.0.2.2:8080}")
    private String allowedOrigins;

    public WebSocketConfig(
            ChatService chatService,
            ObjectMapper objectMapper,
            JwtHandshakeInterceptor jwtHandshakeInterceptor
    ) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Bean
    public WebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler(chatService, objectMapper);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addHandler(chatWebSocketHandler(), "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(origins);
    }
}