package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.entity.Conversation;
import java.util.UUID;

public interface MessagingAccessService {

    Conversation requireParticipant(UUID conversationId, Long userId);

    void validateCanInitiateDirectConversation(Long requesterId, Long targetUserId);

    void validateCanSendMessage(UUID conversationId, Long senderId);

    void validateCanReact(UUID conversationId, Long userId);

    void validateCanSendTyping(UUID conversationId, Long userId);

    Long getOtherParticipantId(UUID conversationId, Long currentUserId);

    boolean isBlockedEitherDirection(Long userA, Long userB);
}
