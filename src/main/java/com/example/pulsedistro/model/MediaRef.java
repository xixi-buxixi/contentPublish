package com.example.pulsedistro.model;

public record MediaRef(
        String mediaId,
        String publicUrl,
        String alt,
        Integer width,
        Integer height
) {
}
