package com.storygen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoryRequest(
        @NotBlank(message = "prompt must not be blank")
        @Size(max = 2000, message = "prompt must not exceed 2000 characters")
        String prompt
) {}
