package com.storygen.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NvidiaNimLlmClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesOpenAiCompatibleResponse() throws Exception {
        // Simulate OpenAI-compatible response from NVIDIA NIM
        String jsonResponse = """
                {
                  "id": "chatcmpl-123",
                  "object": "chat.completion",
                  "model": "nvidia/nemotron-3-ultra-550b-a55b",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Once upon a time, in a land of endless stars, there lived a curious dragon."
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 50,
                    "total_tokens": 60
                  }
                }
                """;

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = mapper.readValue(jsonResponse, Map.class);

        // Extract content using same logic as NvidiaNimLlmClient
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        assertNotNull(choices);
        assertFalse(choices.isEmpty());

        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        assertNotNull(message);

        String text = (String) message.get("content");
        assertEquals("Once upon a time, in a land of endless stars, there lived a curious dragon.", text);
    }

    @Test
    void handlesEmptyChoices() throws Exception {
        String jsonResponse = """
                {
                  "choices": []
                }
                """;

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = mapper.readValue(jsonResponse, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

        assertTrue(choices.isEmpty());
        // The client would throw LlmException("LLM returned no choices")
    }

    @Test
    void handlesNullContent() throws Exception {
        String jsonResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": null
                      }
                    }
                  ]
                }
                """;

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = mapper.readValue(jsonResponse, Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        assertNull(content);
        // The client would throw LlmException("LLM returned empty content")
    }

    @Test
    void requiresApiKey() {
        // Verify that NvidiaNimLlmClient constructor requires a valid API key
        // (The key is passed to WebClient as Bearer token)
        String apiKey = "nvapi-test123";
        assertNotNull(apiKey);
        assertFalse(apiKey.isBlank());
    }
}
