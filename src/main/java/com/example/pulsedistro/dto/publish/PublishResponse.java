package com.example.pulsedistro.dto.publish;

import java.util.List;

public record PublishResponse(
        String taskId,
        String mode,
        List<PublishResultResponse> results
) {
}
