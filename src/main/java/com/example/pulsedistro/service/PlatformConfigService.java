package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.PlatformConfig;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.repository.PlatformConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformConfigService {

    private static final List<String> PLATFORM_ORDER = List.of("xiaohongshu", "zhihu", "wechat", "bilibili");

    private final PlatformConfigRepository repository;
    private final ObjectMapper objectMapper;

    public PlatformConfigService(PlatformConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadDefaultConfigs() {
        for (String platform : PLATFORM_ORDER) {
            PlatformRule rule = readClasspathRule(platform);
            String json = writeRule(rule);
            PlatformConfig config = repository.findByPlatform(platform)
                    .orElseGet(() -> new PlatformConfig(rule.platform(), rule.displayName(), json, true));
            config.update(rule.displayName(), json, true);
            repository.save(config);
        }
    }

    public List<PlatformRule> listEnabledRules() {
        Map<String, Integer> order = PLATFORM_ORDER.stream()
                .collect(Collectors.toMap(platform -> platform, PLATFORM_ORDER::indexOf));

        return repository.findByEnabledTrue().stream()
                .map(this::readRule)
                .sorted(Comparator.comparingInt(rule -> order.getOrDefault(rule.platform(), Integer.MAX_VALUE)))
                .toList();
    }

    public PlatformRule getRule(String platform) {
        String normalized = normalizePlatform(platform);
        return repository.findByPlatform(normalized)
                .filter(PlatformConfig::isEnabled)
                .map(this::readRule)
                .orElseThrow(() -> new BusinessException(404, "platform config not found"));
    }

    private PlatformRule readClasspathRule(String platform) {
        ClassPathResource resource = new ClassPathResource("platforms/" + platform + ".json");
        try {
            String json = resource.getContentAsString(StandardCharsets.UTF_8);
            PlatformRule rule = objectMapper.readValue(json, PlatformRule.class);
            if (!platform.equals(rule.platform())) {
                throw new BusinessException(500, "platform config id mismatch");
            }
            return rule;
        } catch (IOException e) {
            throw new BusinessException(500, "failed to load platform config: " + platform);
        }
    }

    private PlatformRule readRule(PlatformConfig config) {
        try {
            return objectMapper.readValue(config.getConfigJson(), PlatformRule.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to parse platform config: " + config.getPlatform());
        }
    }

    private String writeRule(PlatformRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "failed to serialize platform config: " + rule.platform());
        }
    }

    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            throw new BusinessException(400, "platform is required");
        }
        return platform.trim().toLowerCase(Locale.ROOT);
    }
}
