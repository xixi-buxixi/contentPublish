package com.example.pulsedistro.dto.session;

public record SessionInitResponse(
        long userId,
        String userToken,
        String traceId
) {
}
