package com.example.pulsedistro.dto.plugin;

import java.time.Instant;

public record PluginStatusResponse(
        String userToken,
        boolean online,
        String sessionId,
        String extensionVersion,
        Instant lastHeartbeatAt
) {
}
