package com.example.pulsedistro.dto.plugin;

public record PluginRegisterRequest(
        String sessionId,
        String extensionVersion,
        String browser,
        String clientSessionId
) {
}
