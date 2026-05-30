package com.example.pulsedistro.dto.session;

public record SessionInitRequest(
        String clientType,
        String nickname
) {
}
