package com.example.pulsedistro.dto.adapt;

import com.example.pulsedistro.model.MediaRef;

import java.time.Instant;
import java.util.List;

public record RecordDetailResponse(
        String recordId,
        String taskId,
        String platform,
        String adaptedTitle,
        String adaptedContent,
        List<String> tags,
        List<MediaRef> media,
        String styleExplanation,
        String status,
        String publishMode,
        String publishUrl,
        Instant publishedAt
) {
}
