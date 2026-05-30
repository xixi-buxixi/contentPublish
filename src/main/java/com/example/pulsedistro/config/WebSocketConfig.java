package com.example.pulsedistro.config;

import com.example.pulsedistro.websocket.PipelineWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PipelineWebSocketHandler handler;
    private final String endpoint;

    public WebSocketConfig(
            PipelineWebSocketHandler handler,
            @Value("${pulse.ws.endpoint:/ws/pipeline}") String endpoint
    ) {
        this.handler = handler;
        this.endpoint = endpoint;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, endpoint).setAllowedOriginPatterns("*");
    }
}
