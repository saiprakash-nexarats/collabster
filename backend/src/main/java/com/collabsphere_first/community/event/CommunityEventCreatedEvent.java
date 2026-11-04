package com.collabsphere_first.community.event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;
@Getter public class CommunityEventCreatedEvent extends ApplicationEvent {
    private final UUID communityId; private final UUID eventId; private final Long actorId; private final String title;
    public CommunityEventCreatedEvent(Object source, UUID communityId, UUID eventId, Long actorId, String title) {
        super(source); this.communityId = communityId; this.eventId = eventId; this.actorId = actorId; this.title = title;
    }
}
