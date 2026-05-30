package com.example.pulsedistro.service;

import com.example.pulsedistro.model.ContentBlock;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.model.PlatformRule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class AdaptationModelClient {

    private final Optional<ChatModel> chatModel;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AdaptationModelClient(
            Optional<ChatModel> chatModel,
            ObjectMapper objectMapper,
            @Value("${LANGCHAIN4J_API_KEY:}") String apiKey
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public AdaptationResult adapt(NormalizedContent normalized, PlatformRule rule) {
        if (!StringUtils.hasText(apiKey) || chatModel.isEmpty()) {
            return templateAdapt(normalized, rule, "模板降级：LANGCHAIN4J_API_KEY 缺失，已按平台规则生成。");
        }

        try {
            String response = chatModel.get().chat(prompt(normalized, rule));
            ModelAdaptation modelAdaptation = objectMapper.readValue(extractJson(response), ModelAdaptation.class);
            return new AdaptationResult(
                    limit(modelAdaptation.title(), rule.maxTitleLength()),
                    limit(modelAdaptation.content(), rule.maxContentLength()),
                    normalizeTags(modelAdaptation.tags(), rule.maxTags()),
                    mediaRefs(normalized),
                    StringUtils.hasText(modelAdaptation.styleExplanation())
                            ? modelAdaptation.styleExplanation()
                            : "DeepSeek generated",
                    false
            );
        } catch (RuntimeException | java.io.IOException e) {
            return templateAdapt(normalized, rule, "模板降级：LLM 调用失败，已按平台规则生成。");
        }
    }

    private String prompt(NormalizedContent normalized, PlatformRule rule) {
        return """
                You are adapting Chinese content for %s.
                Return strict JSON only with keys: title, content, tags, styleExplanation.
                Constraints: title <= %d chars, content <= %d chars, tags <= %d.
                Style: %s
                Source title: %s
                Source content:
                %s
                """.formatted(
                rule.displayName(),
                rule.maxTitleLength(),
                rule.maxContentLength(),
                rule.maxTags(),
                rule.stylePrompt(),
                normalized.title(),
                toPlainText(normalized)
        );
    }

    private AdaptationResult templateAdapt(NormalizedContent normalized, PlatformRule rule, String reason) {
        String plainText = toPlainText(normalized);
        String title = limit(normalized.title(), rule.maxTitleLength());
        String content = formatContent(rule.platform(), plainText, rule);
        return new AdaptationResult(
                title,
                limit(content, rule.maxContentLength()),
                defaultTags(rule.platform(), rule.maxTags()),
                mediaRefs(normalized),
                reason,
                true
        );
    }

    private String toPlainText(NormalizedContent normalized) {
        if (normalized.blocks() == null || normalized.blocks().isEmpty()) {
            return normalized.summary() == null ? "" : normalized.summary();
        }

        return normalized.blocks().stream()
                .filter(block -> block.text() != null && !block.text().isBlank())
                .map(ContentBlock::text)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<MediaRef> mediaRefs(NormalizedContent normalized) {
        if (normalized.blocks() == null) {
            return List.of();
        }
        return normalized.blocks().stream()
                .filter(block -> block.media() != null)
                .map(ContentBlock::media)
                .toList();
    }

    private String formatContent(String platform, String plainText, PlatformRule rule) {
        return switch (platform) {
            case "xiaohongshu" -> "✨ " + plainText + "\n\n" + rule.stylePrompt();
            case "zhihu" -> plainText + "\n\n---\n" + rule.stylePrompt();
            case "wechat" -> plainText + "\n\n" + rule.stylePrompt();
            case "bilibili" -> plainText + "\n\n#动态 #专栏\n" + rule.stylePrompt();
            default -> plainText;
        };
    }

    private List<String> defaultTags(String platform, int maxTags) {
        List<String> tags = switch (platform) {
            case "xiaohongshu" -> List.of("内容分发", "效率工具", "自媒体");
            case "zhihu" -> List.of("内容运营", "效率工具");
            case "wechat" -> List.of("内容分发", "公众号");
            case "bilibili" -> List.of("知识分享", "内容运营");
            default -> List.of("内容分发");
        };
        return tags.stream().limit(Math.max(0, maxTags)).toList();
    }

    private List<String> normalizeTags(List<String> tags, int maxTags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(Math.max(0, maxTags))
                .toList();
    }

    private String limit(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String extractJson(String response) {
        String trimmed = response == null ? "" : response.trim();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                trimmed = trimmed.substring(firstLine + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    public record AdaptationResult(
            String title,
            String content,
            List<String> tags,
            List<MediaRef> media,
            String styleExplanation,
            boolean degraded
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ModelAdaptation(
            String title,
            String content,
            List<String> tags,
            String styleExplanation
    ) {
    }
}
