package com.blockwin.protocol_api.hub;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@AllArgsConstructor
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandshakeInterceptor websocketHandshakeInterceptor;
    private final ValidatorWebSocketHandler validatorWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(validatorWebSocketHandler, "/ws/connection")
                .addInterceptors(websocketHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
