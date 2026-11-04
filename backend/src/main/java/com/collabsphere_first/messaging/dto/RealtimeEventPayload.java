package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.MessagingEventType;
import java.time.Instant;
import java.util.UUID;

public record RealtimeEventPayload(
    MessagingEventType eventType,
    UUID conversationId,
    Object data,
    Instant timestamp
) {}
