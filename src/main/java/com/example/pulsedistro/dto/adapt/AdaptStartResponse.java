package com.example.pulsedistro.dto.adapt;

import java.util.List;

public record AdaptStartResponse(
        String taskId,
        String status,
        List<AdaptRecordPlaceholder> records,
        String message
) {
}
