package com.example.pulsedistro.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AdaptExecutorConfig {

    @Bean(name = "adaptTaskExecutor")
    public ThreadPoolTaskExecutor adaptTaskExecutor(
            @Value("${pulse.adapt.executor.core-size:4}") int coreSize,
            @Value("${pulse.adapt.executor.max-size:8}") int maxSize,
            @Value("${pulse.adapt.executor.queue-capacity:64}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int normalizedCoreSize = Math.max(1, coreSize);
        int normalizedMaxSize = Math.max(normalizedCoreSize, maxSize);
        executor.setCorePoolSize(normalizedCoreSize);
        executor.setMaxPoolSize(normalizedMaxSize);
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setThreadNamePrefix("adapt-");
        executor.initialize();
        return executor;
    }
}
