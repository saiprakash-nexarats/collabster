package com.collabsphere_first.messaging.event;

import com.collabsphere_first.messaging.dto.MessageResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DirectMessageCreatedEvent(
    UUID conversationId,
    MessageResponse message,
    Long senderId,
    Long recipientId,
    Instant timestamp
) {
    public DirectMessageCreatedEvent(UUID conversationId, UUID messageId, Long senderId, Long recipientId, Instant timestamp) {
        this(conversationId,
             new MessageResponse(messageId, conversationId, senderId, null, null,
                     com.collabsphere_first.messaging.enums.MessageType.TEXT,
                     null, List.of(), null, List.of(),
                     com.collabsphere_first.messaging.enums.MessageDeliveryState.SENT,
                     com.collabsphere_first.messaging.enums.MessageStatus.ACTIVE,
                     false, timestamp, timestamp, null),
             senderId,
             recipientId,
             timestamp);
    }
}
