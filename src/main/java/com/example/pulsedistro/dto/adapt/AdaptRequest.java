package com.example.pulsedistro.dto.adapt;

import java.util.List;

public record AdaptRequest(
        List<String> platforms,
        boolean forceRegenerate
) {
}
