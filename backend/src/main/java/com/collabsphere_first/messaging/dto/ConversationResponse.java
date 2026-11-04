package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.ConversationType;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    ConversationType type,
    ParticipantSummary otherParticipant,
    MessageSummary lastMessage,
    Instant lastMessageAt,
    long unreadCount,
    boolean notificationsMuted,
    boolean archived,
    Instant createdAt,
    Instant updatedAt
) {}
