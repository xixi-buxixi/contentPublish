package com.example.pulsedistro.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Bean
    @ConditionalOnExpression("'${LANGCHAIN4J_API_KEY:}'.trim().length() > 0")
    public ChatModel deepSeekChatModel(
            @Value("${LANGCHAIN4J_API_KEY:}") String apiKey,
            @Value("${pulse.llm.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${pulse.llm.model-name:deepseek-v4-flash}") String modelName,
            @Value("${pulse.llm.timeout-seconds:30}") long timeoutSeconds
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxRetries(0)
                .temperature(0.7)
                .build();
    }
}
