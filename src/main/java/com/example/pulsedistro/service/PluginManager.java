package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.plugin.PluginHeartbeatRequest;
import com.example.pulsedistro.dto.plugin.PluginPublishStatusRequest;
import com.example.pulsedistro.dto.plugin.PluginPublishStatusResponse;
import com.example.pulsedistro.dto.plugin.PluginRegisterRequest;
import com.example.pulsedistro.dto.plugin.PluginSessionResponse;
import com.example.pulsedistro.dto.plugin.PluginStatusResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginManager {

    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(60);
    private static final Duration HEARTBEAT_RECOVERY_GRACE = Duration.ofMinutes(5);

    private final Map<String, PluginSessionState> sessionsByUserToken = new ConcurrentHashMap<>();
    private final PlatformPublishRecordRepository recordRepository;
    private final PipelineEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public PluginManager(
            PlatformPublishRecordRepository recordRepository,
            PipelineEventPublisher eventPublisher
    ) {
        this(recordRepository, eventPublisher, Clock.systemUTC());
    }

    public PluginManager(
            PlatformPublishRecordRepository recordRepository,
            PipelineEventPublisher eventPublisher,
            Clock clock
    ) {
        this.recordRepository = recordRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PluginSessionResponse register(String userToken, PluginRegisterRequest request) {
        String normalizedToken = requireText(userToken, "userToken is required");
        String sessionId = requireText(request.sessionId(), "sessionId is required");
        Instant now = Instant.now(clock);

        PluginSessionState state = new PluginSessionState(
                normalizedToken,
                sessionId,
                request.extensionVersion(),
                request.browser(),
                "ONLINE",
                now,
                now
        );
        sessionsByUserToken.put(normalizedToken, state);
        publishStatusChanged(state);
        return toSessionResponse(state);
    }

    public PluginSessionResponse heartbeat(String userToken, PluginHeartbeatRequest request) {
        String normalizedToken = requireText(userToken, "userToken is required");
        String normalizedSessionId = requireText(request.sessionId(), "sessionId is required");
        PluginSessionState state = sessionsByUserToken.get(normalizedToken);
        if (state == null || !state.sessionId().equals(normalizedSessionId) || !canHeartbeat(state)) {
            throw new BusinessException(400, "plugin session is offline");
        }

        boolean wasOnline = isOnline(state);
        PluginSessionState refreshed = state.withHeartbeat(Instant.now(clock));
        sessionsByUserToken.put(state.userToken(), refreshed);
        if (!wasOnline) {
            publishStatusChanged(refreshed);
        }
        return toSessionResponse(refreshed);
    }

    public PluginStatusResponse status(String userToken) {
        String normalizedToken = requireText(userToken, "userToken is required");
        PluginSessionState state = sessionsByUserToken.get(normalizedToken);
        if (state == null || !isOnline(state)) {
            return new PluginStatusResponse(normalizedToken, false, null, null, null);
        }

        return new PluginStatusResponse(
                state.userToken(),
                true,
                state.sessionId(),
                state.extensionVersion(),
                state.lastHeartbeatAt()
        );
    }

    public boolean isOnline(String userToken) {
        PluginStatusResponse status = status(userToken);
        return status.online();
    }

    public PluginPublishStatusResponse reportPublishStatus(String userToken, PluginPublishStatusRequest request) {
        requireCurrentSession(userToken, request.sessionId());
        PlatformPublishRecord record = recordRepository.findById(request.recordId())
                .orElseThrow(() -> new BusinessException(404, "record not found"));
        if (!record.getPlatform().equals(request.platform())) {
            throw new BusinessException(400, "platform does not match record");
        }

        String status = normalizeStatus(request.status());
        record.setStatus(status);
        record.setErrorMessage(request.reason());
        if ("SUCCESS".equals(status) || "VERIFIED_SUCCESS".equals(status)) {
            record.setPublishedAt(Instant.now(clock));
        }
        PlatformPublishRecord saved = recordRepository.save(record);

        String normalizedToken = requireText(userToken, "userToken is required");
        eventPublisher.publish(normalizedToken, "PUBLISH_STATUS_CHANGED", Map.of(
                "userToken", normalizedToken,
                "taskId", saved.getTaskId(),
                "recordId", saved.getId(),
                "platform", saved.getPlatform(),
                "status", saved.getStatus(),
                "reason", request.reason() == null ? "" : request.reason()
        ));

        return new PluginPublishStatusResponse(saved.getId(), saved.getStatus());
    }

    private PluginSessionState requireCurrentSession(String userToken, String sessionId) {
        String normalizedToken = requireText(userToken, "userToken is required");
        String normalizedSessionId = requireText(sessionId, "sessionId is required");
        PluginSessionState state = sessionsByUserToken.get(normalizedToken);
        if (state == null || !state.sessionId().equals(normalizedSessionId) || !isOnline(state)) {
            throw new BusinessException(400, "plugin session is offline");
        }
        return state;
    }

    private boolean isOnline(PluginSessionState state) {
        return "ONLINE".equals(state.status())
                && Duration.between(state.lastHeartbeatAt(), Instant.now(clock)).compareTo(HEARTBEAT_TTL) <= 0;
    }

    private boolean canHeartbeat(PluginSessionState state) {
        return "ONLINE".equals(state.status())
                && Duration.between(state.lastHeartbeatAt(), Instant.now(clock)).compareTo(HEARTBEAT_RECOVERY_GRACE) <= 0;
    }

    @Scheduled(fixedDelayString = "${PLUGIN_CLEANUP_INTERVAL_MS:60000}")
    public void cleanupStaleSessions() {
        Instant now = Instant.now(clock);
        sessionsByUserToken.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().lastHeartbeatAt(), now).compareTo(HEARTBEAT_RECOVERY_GRACE) > 0);
    }

    private void publishStatusChanged(PluginSessionState state) {
        eventPublisher.publish(state.userToken(), "PLUGIN_STATUS_CHANGED", Map.of(
                "userToken", state.userToken(),
                "sessionId", state.sessionId(),
                "status", state.status(),
                "lastHeartbeatAt", state.lastHeartbeatAt().toString()
        ));
    }

    private PluginSessionResponse toSessionResponse(PluginSessionState state) {
        return new PluginSessionResponse(
                state.userToken(),
                state.sessionId(),
                state.extensionVersion(),
                state.browser(),
                state.status(),
                state.lastHeartbeatAt()
        );
    }

    private String normalizeStatus(String status) {
        String normalized = requireText(status, "status is required").toUpperCase();
        if (!java.util.List.of("SUCCESS", "VERIFIED_SUCCESS", "WARN", "SUSPENDED", "FAILED").contains(normalized)) {
            throw new BusinessException(400, "unsupported plugin publish status");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private record PluginSessionState(
            String userToken,
            String sessionId,
            String extensionVersion,
            String browser,
            String status,
            Instant lastHeartbeatAt,
            Instant createdAt
    ) {
        PluginSessionState withHeartbeat(Instant heartbeatAt) {
            return new PluginSessionState(userToken, sessionId, extensionVersion, browser, "ONLINE", heartbeatAt, createdAt);
        }
    }
}
