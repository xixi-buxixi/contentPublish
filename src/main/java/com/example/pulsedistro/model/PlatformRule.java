package com.example.pulsedistro.model;

public record PlatformRule(
        String platform,
        String displayName,
        int maxTitleLength,
        int maxContentLength,
        int maxTags,
        boolean supportsMarkdown,
        ImageRule image,
        String stylePrompt
) {
}
