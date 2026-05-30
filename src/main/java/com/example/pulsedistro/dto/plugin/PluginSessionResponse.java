package com.example.pulsedistro.dto.plugin;

import java.time.Instant;

public record PluginSessionResponse(
        String userToken,
        String sessionId,
        String extensionVersion,
        String browser,
        String status,
        Instant lastHeartbeatAt
) {
}
