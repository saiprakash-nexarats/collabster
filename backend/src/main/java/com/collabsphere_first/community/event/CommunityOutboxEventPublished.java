package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class CommunityOutboxEventPublished extends ApplicationEvent {
    private final UUID eventId;
    private final UUID communityId;
    private final String eventType;
    private final String aggregateId;
    private final String payload;
    public CommunityOutboxEventPublished(Object source, UUID eventId, UUID communityId, String eventType, String aggregateId, String payload) {
        super(source); this.eventId = eventId; this.communityId = communityId; this.eventType = eventType; this.aggregateId = aggregateId; this.payload = payload;
    }
}
