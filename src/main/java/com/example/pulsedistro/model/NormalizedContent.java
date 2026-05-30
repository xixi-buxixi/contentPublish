package com.example.pulsedistro.model;

import java.util.List;

public record NormalizedContent(
        String title,
        String summary,
        List<ContentBlock> blocks
) {
}
