package com.example.pulsedistro.model;

import java.util.List;

public record ImageRule(
        int maxCount,
        List<String> acceptedMimeTypes
) {
}
