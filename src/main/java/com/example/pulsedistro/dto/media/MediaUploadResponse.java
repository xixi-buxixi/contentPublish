package com.example.pulsedistro.dto.media;

public record MediaUploadResponse(
        String mediaId,
        String taskId,
        String publicUrl,
        String mimeType,
        long sizeBytes,
        Integer width,
        Integer height,
        String status
) {
}
