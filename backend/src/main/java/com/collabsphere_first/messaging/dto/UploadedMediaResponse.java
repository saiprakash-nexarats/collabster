package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.AttachmentMediaType;

import java.time.Instant;
import java.util.UUID;

public record UploadedMediaResponse(
        UUID id,
        Long ownerUserId,
        String storageKey,
        AttachmentMediaType mediaType,
        String mimeType,
        String fileName,
        Long fileSize,
        String uploadStatus,
        Instant createdAt
) {}
