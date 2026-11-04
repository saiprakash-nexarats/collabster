package com.collabsphere_first.messaging.dto;

import java.util.UUID;

public record AttachmentSignedUrlResponse(
        UUID attachmentId,
        String storageKey,
        String signedUrl,
        int expiresInMinutes
) {}
