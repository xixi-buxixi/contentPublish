package com.example.pulsedistro.dto.media;

public record MediaDeleteResponse(
        String mediaId,
        boolean deleted
) {
}
