package com.collabsphere_first.community.event;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;
@Getter public class CommunityEventRsvpUpdatedEvent extends ApplicationEvent {
    private final UUID communityId; private final UUID eventId; private final Long eventCreatorId; private final Long actorId; private final String status;
    public CommunityEventRsvpUpdatedEvent(Object source, UUID communityId, UUID eventId, Long eventCreatorId, Long actorId, String status) {
        super(source); this.communityId = communityId; this.eventId = eventId; this.eventCreatorId = eventCreatorId; this.actorId = actorId; this.status = status;
    }
}
