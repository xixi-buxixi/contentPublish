package com.example.pulsedistro.dto.task;

import java.time.Instant;

public record TaskSummaryResponse(
        String taskId,
        String title,
        String sourceType,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
