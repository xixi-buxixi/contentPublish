package com.example.pulsedistro.dto.publish;

import com.example.pulsedistro.model.MediaRef;

import java.time.Instant;
import java.util.List;

public record MockPageDataResponse(
        String recordId,
        String platform,
        String title,
        String summary,
        String content,
        List<String> tags,
        List<MediaRef> media,
        Instant publishedAt
) {
}
