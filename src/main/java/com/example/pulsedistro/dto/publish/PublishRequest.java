package com.example.pulsedistro.dto.publish;

import java.util.List;

public record PublishRequest(
        String mode,
        List<String> platforms,
        String clientSessionId
) {
}
