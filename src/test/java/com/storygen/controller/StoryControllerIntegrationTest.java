package com.storygen.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storygen.llm.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createStoryReturnsValidResponse() throws Exception {
        // Arrange
        String mockStory = """
                The Magical Forest
                Deep in the heart of the enchanted woodland, ancient trees whispered secrets to those who listened.
                Moonlight filtered through the canopy like liquid silver.

                A young wanderer stumbled upon a clearing where fireflies danced in impossible patterns.
                """;

        when(llmClient.generateStory(anyString())).thenReturn(mockStory);

        // Act & Assert - POST request
        MvcResult result = mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"a magical forest\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("The Magical Forest"))
                .andExpect(jsonPath("$.htmlContent").exists())
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andReturn();

        // Parse response and verify download URL works
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        String downloadUrl = (String) responseMap.get("downloadUrl");

        // Act & Assert - Download request
        mockMvc.perform(get(downloadUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>The Magical Forest</h1>")));
    }

    @Test
    void createStoryWithBlankPromptReturns400() throws Exception {
        mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createStoryWithMissingPromptReturns400() throws Exception {
        mockMvc.perform(post("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void downloadNonExistentStoryReturns404() throws Exception {
        mockMvc.perform(get("/api/stories/story_nonexist/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadWithMalformedIdReturns404() throws Exception {
        // Path traversal attempt - URL encoded
        mockMvc.perform(get("/api/stories/..%2F..%2Fetc%2Fpasswd/download"))
                .andExpect(status().isNotFound());
    }
}
