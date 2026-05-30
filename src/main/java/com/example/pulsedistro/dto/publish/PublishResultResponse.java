package com.example.pulsedistro.dto.publish;

public record PublishResultResponse(
        String recordId,
        String platform,
        String status,
        String publishUrl
) {
}
