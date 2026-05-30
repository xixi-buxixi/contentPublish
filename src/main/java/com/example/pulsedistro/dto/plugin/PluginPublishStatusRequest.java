package com.example.pulsedistro.dto.plugin;

public record PluginPublishStatusRequest(
        String sessionId,
        String recordId,
        String platform,
        String status,
        String reason,
        String screenshotUrl
) {
}
