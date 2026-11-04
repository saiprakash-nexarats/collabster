package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AttachmentRequest(
        @NotNull(message = "mediaType is required")
        AttachmentMediaType mediaType,

        @NotBlank(message = "storageKey is required")
        @Size(max = 512, message = "storageKey must not exceed 512 characters")
        String storageKey,

        @NotBlank(message = "mimeType is required")
        @Size(max = 128, message = "mimeType must not exceed 128 characters")
        String mimeType,

        @NotBlank(message = "fileName is required")
        @Size(max = 255, message = "fileName must not exceed 255 characters")
        String fileName,

        @NotNull(message = "fileSize is required")
        @Positive(message = "fileSize must be positive")
        Long fileSize,

        Integer width,
        Integer height,
        Integer durationMs,
        Integer displayOrder
) {}
