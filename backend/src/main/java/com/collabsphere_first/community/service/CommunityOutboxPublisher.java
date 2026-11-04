package com.collabsphere_first.community.service;

import com.collabsphere_first.community.event.CommunityOutboxConsumer;
import com.collabsphere_first.community.event.CommunityOutboxEventPublished;
import com.collabsphere_first.community.event.CommunityOutboxMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Publishes every delivered outbox message as a Spring application event. */
@Service @RequiredArgsConstructor
public class CommunityOutboxPublisher implements CommunityOutboxConsumer {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String name() {
        return "spring-community-events";
    }

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public void handle(CommunityOutboxMessage message) {
        eventPublisher.publishEvent(new CommunityOutboxEventPublished(this, message.eventId(), message.communityId(),
                message.eventType(), message.aggregateId(), message.payload()));
    }
}

