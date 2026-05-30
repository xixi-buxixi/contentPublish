package com.example.pulsedistro.websocket;

import com.example.pulsedistro.event.PipelineEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PipelineWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public PipelineWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String userToken = userToken(session);
        if (userToken == null || userToken.isBlank()) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        session.getAttributes().put("userToken", userToken);
        sessionsByUser.computeIfAbsent(userToken, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        Object userToken = session.getAttributes().get("userToken");
        if (userToken == null) {
            return;
        }
        sessionsByUser.computeIfPresent(userToken.toString(), (token, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void sendToUser(String userToken, PipelineEvent event) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userToken);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String payload = toJson(event);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                send(session, payload);
            }
        }
    }

    @Scheduled(fixedDelayString = "${WS_CLEANUP_INTERVAL_MS:60000}")
    public void cleanupClosedSessions() {
        sessionsByUser.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(session -> !session.isOpen());
            return entry.getValue().isEmpty();
        });
    }

    private void send(WebSocketSession session, String payload) {
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ignored) {
                // A later reconnect and HTTP recovery will bring the client back in sync.
            }
        }
    }

    private String userToken(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("userToken");
    }

    private String toJson(PipelineEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize pipeline event", e);
        }
    }
}
