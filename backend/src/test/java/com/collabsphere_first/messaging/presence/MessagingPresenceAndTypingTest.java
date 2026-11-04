package com.collabsphere_first.messaging.presence;

import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.security.MessagingAccessService;
import com.collabsphere_first.messaging.websocket.MessagingRealtimePublisher;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingPresenceAndTypingTest {

    @Mock private ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ZSetOperations<String, String> zSetOperations;
    @Mock private ValueOperations<String, String> valueOperations;

    @Mock private MessagingProperties properties;
    @Mock private MessagingAccessService accessService;
    @Mock private MessagingRealtimePublisher realtimePublisher;
    @Mock private UserRepository userRepository;

    private MessagingPresenceServiceImpl presenceService;
    private MessagingTypingServiceImpl typingService;

    private final Long userId = 50L;
    private final String username = "alice";
    private final UUID conversationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(properties.getPresenceTtl()).thenReturn(Duration.ofSeconds(60));
        lenient().when(properties.getTypingTtl()).thenReturn(Duration.ofSeconds(6));

        presenceService = new MessagingPresenceServiceImpl(redisTemplateProvider, properties);
        typingService = new MessagingTypingServiceImpl(
                redisTemplateProvider, properties, accessService, realtimePublisher, userRepository
        );
    }

    @Test
    @DisplayName("Presence: session connect registers in ZSET and marks user ONLINE")
    void presence_sessionConnectMarksOnline() {
        presenceService.onSessionConnected(userId, username, "session-1");

        verify(zSetOperations).add(eq("collobster:messages:presence:sessions:50"), eq("session-1"), anyDouble());
        verify(valueOperations).set(eq("collobster:messages:presence:50"), eq("ONLINE"), any(Duration.class));
    }

    @Test
    @DisplayName("Presence: heartbeat refreshes score in ZSET and presence key TTL")
    void presence_heartbeatRefreshesZSetScore() {
        presenceService.onHeartbeat(userId, "session-1");

        verify(zSetOperations).add(eq("collobster:messages:presence:sessions:50"), eq("session-1"), anyDouble());
        verify(redisTemplate).expire(eq("collobster:messages:presence:sessions:50"), any(Duration.class));
        verify(valueOperations).set(eq("collobster:messages:presence:50"), eq("ONLINE"), any(Duration.class));
    }

    @Test
    @DisplayName("Presence: multi-device disconnect does NOT mark user offline if sessions remain in ZSET")
    void presence_partialDisconnectKeepsOnline() {
        when(zSetOperations.zCard("collobster:messages:presence:sessions:50")).thenReturn(1L);

        presenceService.onSessionDisconnected(userId, username, "session-1");

        verify(zSetOperations).remove(eq("collobster:messages:presence:sessions:50"), eq("session-1"));
        verify(zSetOperations).removeRangeByScore(eq("collobster:messages:presence:sessions:50"), eq(0.0), anyDouble());
        verify(redisTemplate, never()).delete(eq("collobster:messages:presence:50"));
    }

    @Test
    @DisplayName("Presence: last session disconnect marks user OFFLINE and cleans keys")
    void presence_lastDisconnectMarksOffline() {
        when(zSetOperations.zCard("collobster:messages:presence:sessions:50")).thenReturn(0L);

        presenceService.onSessionDisconnected(userId, username, "session-2");

        verify(zSetOperations).remove(eq("collobster:messages:presence:sessions:50"), eq("session-2"));
        verify(zSetOperations).removeRangeByScore(eq("collobster:messages:presence:sessions:50"), eq(0.0), anyDouble());
        verify(redisTemplate).delete(eq("collobster:messages:presence:50"));
        verify(redisTemplate).delete(eq("collobster:messages:presence:sessions:50"));
    }

    @Test
    @DisplayName("Presence: isUserOnline prunes expired sessions before evaluating count")
    void presence_isUserOnlinePrunesExpired() {
        when(zSetOperations.zCard("collobster:messages:presence:sessions:50")).thenReturn(2L);

        boolean online = presenceService.isUserOnline(userId);

        assertThat(online).isTrue();
        verify(zSetOperations).removeRangeByScore(eq("collobster:messages:presence:sessions:50"), eq(0.0), anyDouble());
    }

    @Test
    @DisplayName("Presence: isUserOnline returns false and deletes key if all sessions expired")
    void presence_isUserOnlineReturnsFalseWhenExpired() {
        when(zSetOperations.zCard("collobster:messages:presence:sessions:50")).thenReturn(0L);

        boolean online = presenceService.isUserOnline(userId);

        assertThat(online).isFalse();
        verify(redisTemplate).delete(eq("collobster:messages:presence:50"));
    }

    @Test
    @DisplayName("Typing: start typing sets key with TTL and publishes TYPING_STARTED")
    void typing_startSetsKeyAndPublishes() {
        Long recipientId = 60L;
        when(accessService.getOtherParticipantId(conversationId, userId)).thenReturn(recipientId);
        User recipient = User.builder().id(recipientId).email("bob@example.com").build();
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        typingService.handleTyping(conversationId, userId, username, true);

        verify(accessService).validateCanSendTyping(conversationId, userId);
        verify(valueOperations).set(eq("collobster:messages:typing:" + conversationId + ":" + userId), eq("1"), eq(Duration.ofSeconds(6)));
        verify(realtimePublisher).publishTyping(conversationId, userId, username, true, "bob@example.com");
    }

    @Test
    @DisplayName("Typing: stop typing deletes key and publishes TYPING_STOPPED")
    void typing_stopDeletesKeyAndPublishes() {
        Long recipientId = 60L;
        when(accessService.getOtherParticipantId(conversationId, userId)).thenReturn(recipientId);
        User recipient = User.builder().id(recipientId).email("bob@example.com").build();
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipient));

        typingService.handleTyping(conversationId, userId, username, false);

        verify(redisTemplate).delete(eq("collobster:messages:typing:" + conversationId + ":" + userId));
        verify(realtimePublisher).publishTyping(conversationId, userId, username, false, "bob@example.com");
    }
}
