package com.storygen.storage;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final Pattern VALID_ID = Pattern.compile("^story_[a-z0-9]{8}$");

    private final Path storageDir;

    public FileStorageService(@Value("${storage.directory}") String storageDirectory) {
        this.storageDir = Paths.get(storageDirectory);
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(storageDir);
    }

    public String generateId() {
        return "story_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public String save(String id, String htmlContent) throws IOException {
        validateId(id);
        Path filePath = storageDir.resolve(id + ".html");
        Files.writeString(filePath, htmlContent);
        log.info("Saved HTML document to {}", filePath);
        return filePath.toString();
    }

    public Resource load(String id) throws IOException {
        validateId(id);
        Path filePath = storageDir.resolve(id + ".html");
        if (!Files.exists(filePath)) {
            return null;
        }
        return new FileSystemResource(filePath);
    }

    private void validateId(String id) {
        if (id == null || !VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid story ID: " + id);
        }
    }
}
