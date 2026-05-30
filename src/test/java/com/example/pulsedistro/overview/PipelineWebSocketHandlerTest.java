package com.example.pulsedistro.overview;

import com.example.pulsedistro.websocket.PipelineWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineWebSocketHandlerTest {

    @Test
    void closesHandshakeMissingUserTokenWithBadData() throws Exception {
        PipelineWebSocketHandler handler = new PipelineWebSocketHandler(new ObjectMapper());
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/pipeline"));
        when(session.getAttributes()).thenReturn(new ConcurrentHashMap<>());

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA);
    }

    @Test
    void removesEmptyUserTokenKeysWhenSessionCloses() {
        PipelineWebSocketHandler handler = new PipelineWebSocketHandler(new ObjectMapper());
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new ConcurrentHashMap<>();
        attributes.put("userToken", "ut_ws_cleanup");
        when(session.getAttributes()).thenReturn(attributes);
        @SuppressWarnings("unchecked")
        Map<String, Set<WebSocketSession>> sessionsByUser =
                (Map<String, Set<WebSocketSession>>) ReflectionTestUtils.getField(handler, "sessionsByUser");
        sessionsByUser.computeIfAbsent("ut_ws_cleanup", ignored -> ConcurrentHashMap.newKeySet()).add(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessionsByUser).doesNotContainKey("ut_ws_cleanup");
    }
}
