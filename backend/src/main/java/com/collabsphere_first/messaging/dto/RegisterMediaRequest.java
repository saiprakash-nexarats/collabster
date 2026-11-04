package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterMediaRequest(
        @NotBlank(message = "storageKey is required")
        String storageKey,

        @NotNull(message = "mediaType is required")
        AttachmentMediaType mediaType,

        @NotBlank(message = "mimeType is required")
        String mimeType,

        @NotBlank(message = "fileName is required")
        String fileName,

        @Positive(message = "fileSize must be positive")
        long fileSize
) {}
