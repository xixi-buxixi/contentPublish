package com.example.pulsedistro.model;

public record ContentBlock(
        String type,
        Integer level,
        String text,
        MediaRef media,
        Boolean ordered,
        Integer depth
) {

    public ContentBlock(String type, Integer level, String text, MediaRef media) {
        this(type, level, text, media, null, null);
    }
}
