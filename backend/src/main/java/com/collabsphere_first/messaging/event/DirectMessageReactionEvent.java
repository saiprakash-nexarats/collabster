package com.collabsphere_first.messaging.event;

import com.collabsphere_first.messaging.dto.ReactionResponse;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageReactionEvent(
    UUID conversationId,
    UUID messageId,
    ReactionResponse reaction,
    boolean removed,
    Long userId,
    Long recipientId,
    Instant timestamp,
    UUID eventId
) {
    public DirectMessageReactionEvent(
        UUID conversationId,
        UUID messageId,
        ReactionResponse reaction,
        boolean removed,
        Long userId,
        Long recipientId,
        Instant timestamp
    ) {
        this(conversationId, messageId, reaction, removed, userId, recipientId, timestamp, null);
    }
}
