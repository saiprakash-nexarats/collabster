package com.collabsphere_first.messaging.dto;

import com.collabsphere_first.messaging.enums.MessageDeliveryState;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    UUID conversationId,
    Long senderId,
    String senderUsername,
    UUID clientMessageId,
    MessageType messageType,
    String body,
    List<AttachmentResponse> attachments,
    ReplyMessagePreview replyTo,
    List<ReactionResponse> reactions,
    MessageDeliveryState deliveryState,
    MessageStatus status,
    boolean deleted,
    Instant createdAt,
    Instant updatedAt,
    Instant editedAt
) {}
