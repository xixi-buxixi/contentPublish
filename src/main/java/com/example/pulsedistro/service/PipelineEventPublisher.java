package com.example.pulsedistro.service;

import com.example.pulsedistro.event.PipelineEvent;
import com.example.pulsedistro.websocket.PipelineWebSocketHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PipelineEventPublisher {

    private static final int MAX_RECENT_EVENTS = 100;
    private static final long RECENT_EVENT_TTL_MILLIS = 30L * 60L * 1000L;

    private final Map<String, ArrayDeque<PipelineEvent>> recentEvents = new ConcurrentHashMap<>();
    private final PipelineWebSocketHandler webSocketHandler;

    public PipelineEventPublisher(PipelineWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public PipelineEvent publish(String userToken, String eventName, Map<String, Object> data) {
        PipelineEvent event = new PipelineEvent(eventName, Instant.now().toEpochMilli(), data);
        recentEvents.compute(userToken, (token, events) -> appendEvent(events, event));
        webSocketHandler.sendToUser(userToken, event);
        return event;
    }

    public List<PipelineEvent> recentEventsFor(String userToken) {
        ArrayDeque<PipelineEvent> events = recentEvents.get(userToken);
        if (events == null) {
            return List.of();
        }
        synchronized (events) {
            return List.copyOf(events);
        }
    }

    private ArrayDeque<PipelineEvent> appendEvent(ArrayDeque<PipelineEvent> events, PipelineEvent event) {
        ArrayDeque<PipelineEvent> target = events == null ? new ArrayDeque<>() : events;
        synchronized (target) {
            target.addLast(event);
            while (target.size() > MAX_RECENT_EVENTS) {
                target.removeFirst();
            }
        }
        return target;
    }

    @Scheduled(fixedDelayString = "${EVENT_CLEANUP_INTERVAL_MS:60000}")
    public void cleanupRecentEvents() {
        long cutoff = Instant.now().toEpochMilli() - RECENT_EVENT_TTL_MILLIS;
        recentEvents.entrySet().removeIf(entry -> {
            ArrayDeque<PipelineEvent> events = entry.getValue();
            synchronized (events) {
                while (!events.isEmpty() && events.peekFirst().timestamp() < cutoff) {
                    events.removeFirst();
                }
                return events.isEmpty();
            }
        });
    }
}
