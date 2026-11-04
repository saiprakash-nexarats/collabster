package com.collabsphere_first.notification.bridge;

import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.event.DirectMessageCreatedEvent;
import com.collabsphere_first.messaging.event.DirectMessageReactionEvent;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.notification.dto.CreateNotificationCommand;
import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.enums.NotificationType;
import com.collabsphere_first.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingNotificationBridge {

    private final NotificationService notificationService;
    private final ConversationParticipantRepository participantRepository;

    @EventListener
    public void onDirectMessageCreated(DirectMessageCreatedEvent event) {
        log.debug("Bridging DirectMessageCreatedEvent (messageId={}, sender={}, recipient={})",
                event.message().id(), event.senderId(), event.recipientId());

        boolean notificationsMuted = participantRepository
                .findByIdConversationIdAndIdUserId(event.conversationId(), event.recipientId())
                .map(ConversationParticipant::isNotificationsMuted)
                .orElse(false);

        // Section 13 & 47: Do NOT store private message body or attachment URLs
        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(event.recipientId())
                .actorUserId(event.senderId())
                .type(NotificationType.DIRECT_MESSAGE)
                .sourceType("MESSAGE")
                .sourceId(event.message().id().toString())
                .contextType("CONVERSATION")
                .contextId(event.conversationId().toString())
                .eventKey("MESSAGE:" + event.message().id() + ":" + event.recipientId())
                .titleKey("notification.message.title")
                .bodyKey("notification.message.body")
                .targetType(NavigationTargetType.OPEN_CONVERSATION)
                .targetId(event.conversationId().toString())
                .payload(Map.of(
                        "conversationId", event.conversationId().toString(),
                        "messageId", event.message().id().toString(),
                        "senderId", event.senderId()
                ))
                .suppressExternalChannels(notificationsMuted)
                .build();

        notificationService.createNotification(cmd);
    }

    @EventListener
    public void onDirectMessageReaction(DirectMessageReactionEvent event) {
        if (event.removed()) {
            return;
        }

        if (event.recipientId() == null || event.userId() == null) {
            return;
        }

        if (event.userId().equals(event.recipientId())) {
            // Self-reaction suppressed
            return;
        }

        log.debug("Bridging DirectMessageReactionEvent (messageId={}, reactor={}, recipient={})",
                event.messageId(), event.userId(), event.recipientId());

        boolean notificationsMuted = participantRepository
                .findByIdConversationIdAndIdUserId(event.conversationId(), event.recipientId())
                .map(ConversationParticipant::isNotificationsMuted)
                .orElse(false);

        String reactionOccurrenceId = event.eventId() != null
                ? event.eventId().toString()
                : (event.messageId() + ":" + event.userId());

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(event.recipientId())
                .actorUserId(event.userId())
                .type(NotificationType.DIRECT_MESSAGE_REACTION)
                .sourceType("MESSAGE")
                .sourceId(event.messageId().toString())
                .contextType("CONVERSATION")
                .contextId(event.conversationId().toString())
                .eventKey("MESSAGE_REACTION:" + reactionOccurrenceId + ":" + event.recipientId())
                .titleKey("notification.message_reaction.title")
                .bodyKey("notification.message_reaction.body")
                .targetType(NavigationTargetType.OPEN_CONVERSATION)
                .targetId(event.conversationId().toString())
                .payload(Map.of(
                        "conversationId", event.conversationId().toString(),
                        "messageId", event.messageId().toString(),
                        "reactorId", event.userId(),
                        "reactionType", event.reaction() != null ? event.reaction().reaction() : ""
                ))
                .suppressExternalChannels(notificationsMuted)
                .build();

        notificationService.createNotification(cmd);
    }
}
