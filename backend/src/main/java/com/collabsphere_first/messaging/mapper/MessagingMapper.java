package com.collabsphere_first.messaging.mapper;

import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.MessageDeliveryState;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.service.MessagingAttachmentService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessagingMapper {

    private final MessagingAttachmentService attachmentService;

    public MessageResponse toMessageResponse(
            DirectMessage message,
            String senderUsername,
            List<MessageAttachment> attachments,
            ReplyMessagePreview replyPreview,
            List<ReactionResponse> reactions,
            MessageDeliveryState deliveryState
    ) {
        if (message == null) {
            return null;
        }

        boolean isDeleted = message.isDeleted();
        List<AttachmentResponse> attachmentResponses = isDeleted
                ? List.of()
                : (attachments != null ? attachments.stream().map(attachmentService::toResponse).toList() : List.of());

        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                senderUsername,
                message.getClientMessageId(),
                message.getMessageType(),
                isDeleted ? null : message.getBody(),
                attachmentResponses,
                replyPreview,
                reactions != null ? reactions : List.of(),
                deliveryState != null ? deliveryState : MessageDeliveryState.SENT,
                message.getStatus(),
                isDeleted,
                message.getCreatedAt(),
                message.getUpdatedAt(),
                message.getEditedAt()
        );
    }

    public ReplyMessagePreview toReplyPreview(DirectMessage replyToMessage, String senderUsername) {
        if (replyToMessage == null) {
            return null;
        }
        boolean isDeleted = replyToMessage.isDeleted();
        String bodyPreview = isDeleted ? "Message deleted" : previewText(replyToMessage);
        return new ReplyMessagePreview(
                replyToMessage.getId(),
                replyToMessage.getSenderId(),
                senderUsername,
                bodyPreview,
                replyToMessage.getMessageType(),
                isDeleted,
                replyToMessage.getCreatedAt()
        );
    }

    public MessageSummary toMessageSummary(DirectMessage message) {
        if (message == null) {
            return null;
        }
        boolean isDeleted = message.isDeleted();
        String preview = isDeleted ? "Message deleted" : previewText(message);
        return new MessageSummary(
                message.getId(),
                message.getSenderId(),
                preview,
                message.getMessageType(),
                isDeleted,
                message.getCreatedAt()
        );
    }

    public ParticipantSummary toParticipantSummary(User user, boolean online) {
        if (user == null) {
            return null;
        }
        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "")).trim();
        return new ParticipantSummary(
                user.getId(),
                user.getUsername(),
                fullName.isBlank() ? user.getUsername() : fullName,
                user.getProfilePicture(),
                online
        );
    }

    public ConversationResponse toConversationResponse(
            Conversation conversation,
            ParticipantSummary otherParticipant,
            MessageSummary lastMessage,
            long unreadCount,
            boolean notificationsMuted,
            boolean archived
    ) {
        if (conversation == null) {
            return null;
        }
        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                otherParticipant,
                lastMessage,
                conversation.getLastMessageAt(),
                unreadCount,
                notificationsMuted,
                archived,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public ReactionResponse toReactionResponse(MessageReaction reaction, String username) {
        if (reaction == null) {
            return null;
        }
        return new ReactionResponse(
                reaction.getId(),
                reaction.getUserId(),
                username,
                reaction.getReaction(),
                reaction.getCreatedAt()
        );
    }

    private String previewText(DirectMessage message) {
        if (message.getBody() != null && !message.getBody().isBlank()) {
            String b = message.getBody().trim();
            return b.length() > 60 ? b.substring(0, 57) + "..." : b;
        }
        if (message.getMessageType() == MessageType.ATTACHMENT) {
            return "[Attachment]";
        }
        return "";
    }
}
