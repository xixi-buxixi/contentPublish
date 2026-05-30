package com.example.pulsedistro.service;

import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JsonContentMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<MediaRef>> MEDIA_REF_LIST = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public JsonContentMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NormalizedContent readNormalized(String normalizedJson) {
        try {
            return objectMapper.readValue(normalizedJson, NormalizedContent.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to deserialize normalized content");
        }
    }

    public List<String> readTags(String tagsJson) {
        try {
            return objectMapper.readValue(tagsJson, STRING_LIST);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to deserialize tags");
        }
    }

    public List<MediaRef> readMediaRefs(String mediaJson) {
        try {
            return objectMapper.readValue(mediaJson, MEDIA_REF_LIST);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to deserialize media refs");
        }
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to serialize content");
        }
    }
}
