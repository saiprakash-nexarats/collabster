package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.messaging.dto.MessageResponse;
import com.collabsphere_first.messaging.dto.ReactionResponse;

import java.time.Instant;
import java.util.UUID;

public interface MessagingRealtimePublisher {

    void publishMessageCreated(MessageResponse message, String recipientUserPrincipal, String senderUserPrincipal);

    void publishMessageUpdated(MessageResponse message, String recipientUserPrincipal, String senderUserPrincipal);

    void publishMessageDeleted(UUID conversationId, UUID messageId, String recipientUserPrincipal, String senderUserPrincipal);

    void publishReactionUpdated(UUID conversationId, UUID messageId, ReactionResponse reaction, boolean removed, String recipientUserPrincipal, String senderUserPrincipal);

    void publishDeliveryReceipt(UUID conversationId, UUID messageId, Instant deliveredAt, String senderUserPrincipal);

    void publishReadReceipt(UUID conversationId, UUID lastReadMessageId, Instant readAt, String senderUserPrincipal);

    void publishTyping(UUID conversationId, Long userId, String username, boolean typing, String recipientUserPrincipal);

    void publishPresence(Long userId, String username, boolean online, String targetUserPrincipal);
}
