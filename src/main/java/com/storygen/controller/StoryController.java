package com.storygen.controller;

import com.storygen.convert.HtmlDocumentBuilder;
import com.storygen.convert.StoryToHtmlConverter;
import com.storygen.dto.HtmlDocument;
import com.storygen.dto.StoryRequest;
import com.storygen.dto.StoryResponse;
import com.storygen.llm.LlmClient;
import com.storygen.storage.FileStorageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private static final Logger log = LoggerFactory.getLogger(StoryController.class);

    private final LlmClient llmClient;
    private final StoryToHtmlConverter converter;
    private final HtmlDocumentBuilder documentBuilder;
    private final FileStorageService storageService;

    public StoryController(LlmClient llmClient,
                           StoryToHtmlConverter converter,
                           HtmlDocumentBuilder documentBuilder,
                           FileStorageService storageService) {
        this.llmClient = llmClient;
        this.converter = converter;
        this.documentBuilder = documentBuilder;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<StoryResponse> createStory(@Valid @RequestBody StoryRequest request) {
        String rawStory = llmClient.generateStory(request.prompt());
        HtmlDocument doc = converter.convert(rawStory);
        String html = documentBuilder.build(doc);

        String id = storageService.generateId();
        try {
            storageService.save(id, html);
        } catch (IOException e) {
            log.error("Failed to save HTML for story {}", id, e);
            throw new RuntimeException("Failed to persist story", e);
        }

        StoryResponse response = new StoryResponse(
                id,
                doc.title(),
                html,
                "/api/stories/" + id + "/download"
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadStory(@PathVariable String id) throws IOException {
        Resource resource = storageService.load(id);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + id + ".html\"")
                .body(resource);
    }
}
