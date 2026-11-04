package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    AttachmentMediaType mediaType,
    String storageKey,
    String url,
    String mimeType,
    String fileName,
    long fileSize,
    Integer width,
    Integer height,
    Integer durationMs,
    int displayOrder,
    Instant createdAt
) {}
