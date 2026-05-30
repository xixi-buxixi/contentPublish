package com.example.pulsedistro.dto.task;

public record CreateTaskRequest(
        String title,
        String sourceType,
        String rawContent
) {
}
