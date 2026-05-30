package com.example.pulsedistro.event;

import java.util.Map;

public record PipelineEvent(
        String event,
        long timestamp,
        Map<String, Object> data
) {
}
