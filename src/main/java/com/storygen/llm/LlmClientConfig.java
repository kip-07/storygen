package com.storygen.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmClientConfig {

    @Bean
    public LlmClient llmClient(
            @Value("${llm.nvidia.base-url}") String baseUrl,
            @Value("${llm.nvidia.api-key}") String apiKey,
            @Value("${llm.nvidia.model}") String model,
            @Value("${llm.nvidia.max-tokens:16384}") int maxTokens) {
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("llm.nvidia.api-key must be set");
        }
        return new NvidiaNimLlmClient(baseUrl, apiKey, model, maxTokens);
    }
}
