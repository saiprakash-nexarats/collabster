package com.collabsphere_first.messaging.websocket;

import com.collabsphere_first.messaging.dto.MessageResponse;
import com.collabsphere_first.messaging.dto.ReactionResponse;
import com.collabsphere_first.messaging.dto.RealtimeEventPayload;
import com.collabsphere_first.messaging.enums.MessagingEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingRealtimePublisherImpl implements MessagingRealtimePublisher {

    private static final String QUEUE_MESSAGES = "/queue/messages";
    private static final String QUEUE_MESSAGE_EVENTS = "/queue/message-events";
    private static final String QUEUE_RECEIPTS = "/queue/receipts";
    private static final String QUEUE_TYPING = "/queue/typing";

    private final SimpMessagingTemplate messagingTemplate;

    private void publishAfterCommit(Runnable publishAction) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        publishAction.run();
                    } catch (Exception ex) {
                        log.error("Failed to publish realtime messaging event after commit: {}", ex.getMessage(), ex);
                    }
                }
            });
        } else {
            try {
                publishAction.run();
            } catch (Exception ex) {
                log.error("Failed to publish realtime messaging event immediately: {}", ex.getMessage(), ex);
            }
        }
    }

    private void sendToUserSafe(String userPrincipal, String destination, Object payload) {
        if (userPrincipal == null || userPrincipal.isBlank()) {
            return;
        }
        try {
            messagingTemplate.convertAndSendToUser(userPrincipal, destination, payload);
            log.debug("Sent realtime payload to user {} at {}", userPrincipal, destination);
        } catch (Exception ex) {
            log.warn("Failed to deliver realtime payload to user {} at destination {}: {}",
                    userPrincipal, destination, ex.getMessage());
        }
    }

    @Override
    public void publishMessageCreated(MessageResponse message, String recipientUserPrincipal, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.MESSAGE_CREATED,
                    message.conversationId(),
                    message,
                    Instant.now()
            );
            if (recipientUserPrincipal != null) {
                sendToUserSafe(recipientUserPrincipal, QUEUE_MESSAGES, payload);
            }
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_MESSAGES, payload);
            }
        });
    }

    @Override
    public void publishMessageUpdated(MessageResponse message, String recipientUserPrincipal, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.MESSAGE_UPDATED,
                    message.conversationId(),
                    message,
                    Instant.now()
            );
            if (recipientUserPrincipal != null) {
                sendToUserSafe(recipientUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
        });
    }

    @Override
    public void publishMessageDeleted(UUID conversationId, UUID messageId, String recipientUserPrincipal, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            Map<String, Object> data = Map.of(
                    "messageId", messageId,
                    "deleted", true
            );
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.MESSAGE_DELETED,
                    conversationId,
                    data,
                    Instant.now()
            );
            if (recipientUserPrincipal != null) {
                sendToUserSafe(recipientUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
        });
    }

    @Override
    public void publishReactionUpdated(UUID conversationId, UUID messageId, ReactionResponse reaction, boolean removed,
                                       String recipientUserPrincipal, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            Map<String, Object> data = Map.of(
                    "messageId", messageId,
                    "reaction", reaction != null ? reaction : Map.of(),
                    "removed", removed
            );
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.REACTION_UPDATED,
                    conversationId,
                    data,
                    Instant.now()
            );
            if (recipientUserPrincipal != null) {
                sendToUserSafe(recipientUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
            }
        });
    }

    @Override
    public void publishDeliveryReceipt(UUID conversationId, UUID messageId, Instant deliveredAt, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            Map<String, Object> data = Map.of(
                    "messageId", messageId,
                    "deliveredAt", deliveredAt
            );
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.MESSAGE_DELIVERED,
                    conversationId,
                    data,
                    Instant.now()
            );
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_RECEIPTS, payload);
            }
        });
    }

    @Override
    public void publishReadReceipt(UUID conversationId, UUID lastReadMessageId, Instant readAt, String senderUserPrincipal) {
        publishAfterCommit(() -> {
            Map<String, Object> data = Map.of(
                    "lastReadMessageId", lastReadMessageId,
                    "readAt", readAt
            );
            RealtimeEventPayload payload = new RealtimeEventPayload(
                    MessagingEventType.CONVERSATION_READ,
                    conversationId,
                    data,
                    Instant.now()
            );
            if (senderUserPrincipal != null) {
                sendToUserSafe(senderUserPrincipal, QUEUE_RECEIPTS, payload);
            }
        });
    }

    @Override
    public void publishTyping(UUID conversationId, Long userId, String username, boolean typing, String recipientUserPrincipal) {
        // Typing is ephemeral, immediately published without waiting for DB commit
        Map<String, Object> data = Map.of(
                "userId", userId,
                "username", username,
                "typing", typing
        );
        RealtimeEventPayload payload = new RealtimeEventPayload(
                typing ? MessagingEventType.TYPING_STARTED : MessagingEventType.TYPING_STOPPED,
                conversationId,
                data,
                Instant.now()
        );
        if (recipientUserPrincipal != null) {
            sendToUserSafe(recipientUserPrincipal, QUEUE_TYPING, payload);
        }
    }

    @Override
    public void publishPresence(Long userId, String username, boolean online, String targetUserPrincipal) {
        // Presence is ephemeral, immediately published
        Map<String, Object> data = Map.of(
                "userId", userId,
                "username", username,
                "online", online
        );
        RealtimeEventPayload payload = new RealtimeEventPayload(
                MessagingEventType.PRESENCE_CHANGED,
                null,
                data,
                Instant.now()
        );
        if (targetUserPrincipal != null) {
            sendToUserSafe(targetUserPrincipal, QUEUE_MESSAGE_EVENTS, payload);
        }
    }
}
