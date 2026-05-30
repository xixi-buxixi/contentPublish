package com.example.pulsedistro.overview;

import com.example.pulsedistro.event.PipelineEvent;
import com.example.pulsedistro.service.PipelineEventPublisher;
import com.example.pulsedistro.websocket.PipelineWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineEventPublisherTest {

    @Test
    void cleanupRemovesExpiredEventsAndEmptyUserQueues() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-30T00:00:00Z"));
        PipelineEventPublisher publisher = new PipelineEventPublisher(
                new PipelineWebSocketHandler(new ObjectMapper()),
                clock,
                Duration.ofMillis(100)
        );

        publisher.publish("ut_old", "PUBLISH_STATUS_CHANGED", Map.of("taskId", "old"));
        clock.advance(Duration.ofMillis(150));
        publisher.publish("ut_fresh", "PUBLISH_STATUS_CHANGED", Map.of("taskId", "fresh"));

        publisher.cleanupRecentEvents();

        assertThat(publisher.recentEventsFor("ut_old")).isEmpty();
        assertThat(publisher.recentEventsFor("ut_fresh"))
                .extracting(PipelineEvent::event)
                .containsExactly("PUBLISH_STATUS_CHANGED");
    }

    private static class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
