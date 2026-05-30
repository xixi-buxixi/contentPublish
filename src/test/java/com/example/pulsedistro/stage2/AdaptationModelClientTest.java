package com.example.pulsedistro.stage2;

import com.example.pulsedistro.model.ContentBlock;
import com.example.pulsedistro.model.ImageRule;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.service.AdaptationModelClient;
import com.example.pulsedistro.service.AdaptationModelClient.AdaptationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptationModelClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlatformRule rule = new PlatformRule(
            "xiaohongshu",
            "小红书",
            20,
            500,
            3,
            false,
            new ImageRule(9, List.of("image/png")),
            "活泼、真实、带标签"
    );
    private final NormalizedContent normalized = new NormalizedContent(
            "原始标题",
            "原始摘要",
            List.of(new ContentBlock("paragraph", null, "这是原始正文", null))
    );

    @Test
    void usesConfiguredChatModelWhenApiKeyIsPresent() {
        ChatModel chatModel = mock(ChatModel.class);
        when(chatModel.chat(anyString())).thenReturn("""
                {"title":"DeepSeek 标题","content":"DeepSeek 正文","tags":["AI","分发"],"styleExplanation":"DeepSeek generated"}
                """);
        AdaptationModelClient client = new AdaptationModelClient(
                Optional.of(chatModel),
                objectMapper,
                "configured-key"
        );

        AdaptationResult result = client.adapt(normalized, rule);

        assertThat(result.degraded()).isFalse();
        assertThat(result.title()).isEqualTo("DeepSeek 标题");
        assertThat(result.content()).isEqualTo("DeepSeek 正文");
        assertThat(result.tags()).containsExactly("AI", "分发");
        verify(chatModel).chat(anyString());
    }

    @Test
    void fallsBackToTemplateWhenKeyIsMissingOrModelThrows() {
        ChatModel chatModel = mock(ChatModel.class);
        AdaptationModelClient missingKeyClient = new AdaptationModelClient(
                Optional.of(chatModel),
                objectMapper,
                ""
        );

        AdaptationResult missingKey = missingKeyClient.adapt(normalized, rule);

        assertThat(missingKey.degraded()).isTrue();
        assertThat(missingKey.content()).contains("这是原始正文");
        assertThat(missingKey.styleExplanation()).contains("模板降级");
        verify(chatModel, never()).chat(anyString());

        when(chatModel.chat(anyString())).thenThrow(new IllegalStateException("boom"));
        AdaptationModelClient throwingClient = new AdaptationModelClient(
                Optional.of(chatModel),
                objectMapper,
                "configured-key"
        );

        AdaptationResult thrown = throwingClient.adapt(normalized, rule);

        assertThat(thrown.degraded()).isTrue();
        assertThat(thrown.content()).contains("这是原始正文");
        assertThat(thrown.styleExplanation()).contains("模板降级");
    }
}
