package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.entity.Conversation;
import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.exception.BlockedUserException;
import com.collabsphere_first.messaging.exception.ConversationNotFoundException;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessagingAccessServiceImpl implements MessagingAccessService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final NetworkAccessService networkAccessService;

    @Override
    public Conversation requireParticipant(UUID conversationId, Long userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        boolean isParticipant = participantRepository.existsByIdConversationIdAndIdUserId(conversationId, userId);
        if (!isParticipant) {
            log.warn("Access denied: user {} is not a participant in conversation {}", userId, conversationId);
            throw new MessagingAccessDeniedException("You are not a participant in this conversation");
        }

        return conversation;
    }

    @Override
    public void validateCanInitiateDirectConversation(Long requesterId, Long targetUserId) {
        if (Objects.equals(requesterId, targetUserId)) {
            throw new IllegalArgumentException("Cannot create a direct conversation with yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        if (targetUser.getAccountEnabled() != null && !targetUser.getAccountEnabled()) {
            throw new IllegalArgumentException("Target user account is disabled");
        }

        if (networkAccessService.isBlockedEitherDirection(requesterId, targetUserId)) {
            throw new BlockedUserException("Cannot start conversation: a block relationship exists with this user");
        }
    }

    @Override
    public void validateCanSendMessage(UUID conversationId, Long senderId) {
        Conversation conversation = requireParticipant(conversationId, senderId);

        if (conversation.getType() == ConversationType.DIRECT) {
            Long otherUserId = getOtherParticipantId(conversationId, senderId);
            if (otherUserId != null && networkAccessService.isBlockedEitherDirection(senderId, otherUserId)) {
                throw new BlockedUserException("Cannot send message: a block relationship exists with this user");
            }
        }
    }

    @Override
    public void validateCanReact(UUID conversationId, Long userId) {
        Conversation conversation = requireParticipant(conversationId, userId);

        if (conversation.getType() == ConversationType.DIRECT) {
            Long otherUserId = getOtherParticipantId(conversationId, userId);
            if (otherUserId != null && networkAccessService.isBlockedEitherDirection(userId, otherUserId)) {
                throw new BlockedUserException("Cannot react to message: a block relationship exists with this user");
            }
        }
    }

    @Override
    public void validateCanSendTyping(UUID conversationId, Long userId) {
        Conversation conversation = requireParticipant(conversationId, userId);

        if (conversation.getType() == ConversationType.DIRECT) {
            Long otherUserId = getOtherParticipantId(conversationId, userId);
            if (otherUserId != null && networkAccessService.isBlockedEitherDirection(userId, otherUserId)) {
                throw new BlockedUserException("Cannot send typing indicator: a block relationship exists with this user");
            }
        }
    }

    @Override
    public Long getOtherParticipantId(UUID conversationId, Long currentUserId) {
        List<ConversationParticipant> participants = participantRepository.findByIdConversationId(conversationId);
        return participants.stream()
                .map(p -> p.getId().getUserId())
                .filter(uid -> !Objects.equals(uid, currentUserId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean isBlockedEitherDirection(Long userA, Long userB) {
        return networkAccessService.isBlockedEitherDirection(userA, userB);
    }
}
