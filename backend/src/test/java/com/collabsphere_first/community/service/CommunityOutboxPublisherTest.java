package com.collabsphere_first.community.service;

import com.collabsphere_first.community.event.CommunityOutboxEventPublished;
import com.collabsphere_first.community.event.CommunityOutboxMessage;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommunityOutboxPublisherTest {
    @Test
    void supportsAndPublishesEveryCommunityEventType() {
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        CommunityOutboxPublisher publisher = new CommunityOutboxPublisher(events);
        CommunityOutboxMessage message = new CommunityOutboxMessage(UUID.randomUUID(), UUID.randomUUID(),
                "POST_CREATED", "post-1", "{\"authorId\":1}");

        org.junit.jupiter.api.Assertions.assertTrue(publisher.supports(message.eventType()));
        publisher.handle(message);

        verify(events).publishEvent(any(CommunityOutboxEventPublished.class));
    }
}
