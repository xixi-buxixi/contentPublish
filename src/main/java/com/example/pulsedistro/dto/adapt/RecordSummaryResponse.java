package com.example.pulsedistro.dto.adapt;

public record RecordSummaryResponse(
        String recordId,
        String taskId,
        String platform,
        String adaptedTitle,
        String status,
        String publishMode,
        String publishUrl
) {
}
