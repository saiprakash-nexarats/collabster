package com.collabsphere_first.messaging.presence;

import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.security.MessagingAccessService;
import com.collabsphere_first.messaging.websocket.MessagingRealtimePublisher;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingTypingServiceImpl implements MessagingTypingService {

    private static final String TYPING_PREFIX = "collobster:messages:typing:";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final MessagingProperties messagingProperties;
    private final MessagingAccessService messagingAccessService;
    private final MessagingRealtimePublisher realtimePublisher;
    private final UserRepository userRepository;

    @Override
    public void handleTyping(UUID conversationId, Long senderId, String senderUsername, boolean typing) {
        if (conversationId == null || senderId == null) {
            return;
        }

        // Authorization check: participant + non-blocked
        messagingAccessService.validateCanSendTyping(conversationId, senderId);

        Long recipientUserId = messagingAccessService.getOtherParticipantId(conversationId, senderId);
        if (recipientUserId == null) {
            return;
        }

        String recipientPrincipal = userRepository.findById(recipientUserId)
                .map(User::getEmail)
                .orElse(null);

        // Update ephemeral Redis state
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            try {
                String key = TYPING_PREFIX + conversationId + ":" + senderId;
                if (typing) {
                    Duration ttl = messagingProperties.getTypingTtl();
                    redis.opsForValue().set(key, "1", ttl);
                } else {
                    redis.delete(key);
                }
            } catch (Exception ex) {
                log.warn("Failed to update Redis typing state: {}", ex.getMessage());
            }
        }

        // Broadcast typing event to recipient
        realtimePublisher.publishTyping(conversationId, senderId, senderUsername, typing, recipientPrincipal);
    }

    @Override
    public boolean isTyping(UUID conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return false;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String key = TYPING_PREFIX + conversationId + ":" + userId;
            return Boolean.TRUE.equals(redis.hasKey(key));
        } catch (Exception ex) {
            log.warn("Failed to check Redis typing state: {}", ex.getMessage());
            return false;
        }
    }
}
