package com.example.pulsedistro.stage4;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.plugin.PluginHeartbeatRequest;
import com.example.pulsedistro.dto.plugin.PluginPublishStatusRequest;
import com.example.pulsedistro.dto.plugin.PluginRegisterRequest;
import com.example.pulsedistro.dto.plugin.PluginSessionResponse;
import com.example.pulsedistro.dto.plugin.PluginStatusResponse;
import com.example.pulsedistro.event.PipelineEvent;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import com.example.pulsedistro.service.PipelineEventPublisher;
import com.example.pulsedistro.service.PluginManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class PluginAgentStageFourIntegrationTest {

    @Autowired
    private PluginManager pluginManager;

    @Autowired
    private PipelineEventPublisher eventPublisher;

    @Autowired
    private PlatformPublishRecordRepository recordRepository;

    @Test
    void pluginRegisterHeartbeatStatusAndPublishCallbackStayScopedByUserToken() {
        String userToken = "ut_stage4_demo";

        PluginSessionResponse registered = pluginManager.register(userToken, new PluginRegisterRequest(
                "plugin-session-001",
                "1.0.0",
                "Chrome",
                "web-session-001"
        ));

        assertThat(registered.userToken()).isEqualTo(userToken);
        assertThat(registered.status()).isEqualTo("ONLINE");
        assertThat(pluginManager.status(userToken).online()).isTrue();

        PluginSessionResponse replacement = pluginManager.register(userToken, new PluginRegisterRequest(
                "plugin-session-002",
                "1.0.1",
                "Chrome",
                "web-session-001"
        ));
        PluginStatusResponse status = pluginManager.status(userToken);

        assertThat(replacement.sessionId()).isEqualTo("plugin-session-002");
        assertThat(status.sessionId()).isEqualTo("plugin-session-002");
        assertThat(status.extensionVersion()).isEqualTo("1.0.1");

        PluginSessionResponse heartbeat = pluginManager.heartbeat(userToken, new PluginHeartbeatRequest("plugin-session-002"));
        assertThat(heartbeat.status()).isEqualTo("ONLINE");

        PlatformPublishRecord record = recordRepository.save(new PlatformPublishRecord("task-stage4", "xiaohongshu"));
        record.setPublishMode("real");
        record.setStatus("PUBLISHING");
        record = recordRepository.save(record);

        pluginManager.reportPublishStatus(userToken, new PluginPublishStatusRequest(
                "plugin-session-002",
                record.getId(),
                "xiaohongshu",
                "SUSPENDED",
                "CAPTCHA_REQUIRED",
                "http://localhost:8080/media/screenshot"
        ));

        PlatformPublishRecord updated = recordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("SUSPENDED");
        assertThat(updated.getErrorMessage()).isEqualTo("CAPTCHA_REQUIRED");

        List<PipelineEvent> events = eventPublisher.recentEventsFor(userToken);
        assertThat(events).extracting(PipelineEvent::event)
                .contains("PLUGIN_STATUS_CHANGED", "PUBLISH_STATUS_CHANGED");
        assertThat(events.getLast().data()).containsEntry("status", "SUSPENDED");
    }

    @Test
    void heartbeatRevivesSameRecentlyExpiredSessionButRejectsOldOrDifferentSessions() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-30T10:00:00Z"));
        PluginManager manager = new PluginManager(
                mock(PlatformPublishRecordRepository.class),
                mock(PipelineEventPublisher.class),
                clock
        );
        String userToken = "ut_plugin_grace";

        manager.register(userToken, new PluginRegisterRequest(
                "session-one",
                "1.0.0",
                "Chrome",
                "web-session"
        ));
        clock.advance(Duration.ofSeconds(90));

        assertThat(manager.status(userToken).online()).isFalse();
        assertThat(manager.heartbeat(userToken, new PluginHeartbeatRequest("session-one")).status())
                .isEqualTo("ONLINE");

        assertThatThrownBy(() -> manager.heartbeat(userToken, new PluginHeartbeatRequest("different-session")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);

        clock.advance(Duration.ofMinutes(6));
        assertThatThrownBy(() -> manager.heartbeat(userToken, new PluginHeartbeatRequest("session-one")))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
