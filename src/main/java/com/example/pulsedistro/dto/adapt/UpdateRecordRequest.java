package com.example.pulsedistro.dto.adapt;

import java.util.List;

public record UpdateRecordRequest(
        String adaptedTitle,
        String adaptedContent,
        List<String> tags,
        List<String> mediaIds
) {
}
