package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.MessageType;
import java.time.Instant;
import java.util.UUID;

public record ReplyMessagePreview(
    UUID id,
    Long senderId,
    String senderUsername,
    String bodyPreview,
    MessageType messageType,
    boolean deleted,
    Instant createdAt
) {}
