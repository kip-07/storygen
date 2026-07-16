package com.storygen.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * LLM Client for NVIDIA NIM API (OpenAI-compatible endpoint).
 * 
 * Uses the /chat/completions endpoint with the standard OpenAI request format.
 */
public class NvidiaNimLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(NvidiaNimLlmClient.class);

    private final WebClient webClient;
    private final String model;
    private final int maxTokens;

    /**
     * @param baseUrl  NVIDIA NIM base URL (e.g., "https://integrate.api.nvidia.com/v1")
     * @param apiKey   NVIDIA API key (nvapi-...)
     * @param model    Model identifier (e.g., "nvidia/nemotron-3-ultra-550b-a55b")
     * @param maxTokens Maximum tokens for the response
     */
    public NvidiaNimLlmClient(String baseUrl, String apiKey, String model, int maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String generateStory(String prompt) {
        try {
            // NVIDIA NIM uses OpenAI-compatible chat completions format
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 1.0,
                    "top_p", 0.95,
                    "max_tokens", maxTokens,
                    "stream", false
            );

            String responseJson = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            return extractResponseText(responseJson);
        } catch (WebClientResponseException e) {
            log.error("NVIDIA NIM returned non-2xx response: {}", e.getStatusCode());
            throw new LlmException("LLM backend returned error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call NVIDIA NIM", e);
            throw new LlmException("LLM backend unavailable: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(String responseJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> responseMap = mapper.readValue(responseJson, Map.class);

            // OpenAI-compatible response structure: { choices: [{ message: { content: "..." } }] }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LlmException("LLM returned no choices");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
            if (message == null) {
                throw new LlmException("LLM returned no message in choice");
            }

            String text = (String) message.get("content");
            if (text == null || text.isBlank()) {
                throw new LlmException("LLM returned empty content");
            }
            return text;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Failed to parse LLM response: " + e.getMessage(), e);
        }
    }
}
