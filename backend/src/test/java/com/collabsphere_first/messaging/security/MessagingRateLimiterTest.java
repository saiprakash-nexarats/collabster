package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.exception.MessagingRateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagingRateLimiterTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private MessagingProperties properties;
    private MessagingRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        properties = new MessagingProperties();
        properties.getRateLimit().setMessagesPerMinute(3);
        properties.getRateLimit().setConversationsPerHour(2);

        when(redisTemplateProvider.getIfAvailable()).thenReturn(null); // test local fallback
        rateLimiter = new MessagingRateLimiter(redisTemplateProvider, properties);
    }

    @Test
    @DisplayName("Rate Limiter: permits requests within limit")
    void permitsWithinLimit() {
        Long userId = 999L;
        assertThatCode(() -> {
            rateLimiter.checkMessageSend(userId);
            rateLimiter.checkMessageSend(userId);
            rateLimiter.checkMessageSend(userId);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rate Limiter: throws MessagingRateLimitExceededException when capacity exceeded")
    void throwsWhenExceeded() {
        Long userId = 888L;
        rateLimiter.checkConversationCreation(userId);
        rateLimiter.checkConversationCreation(userId);

        assertThatThrownBy(() -> rateLimiter.checkConversationCreation(userId))
                .isInstanceOf(MessagingRateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded for conversation-create");
    }
}
