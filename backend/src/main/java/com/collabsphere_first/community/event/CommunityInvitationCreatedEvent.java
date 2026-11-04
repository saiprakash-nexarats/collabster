package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;

@Getter
public class CommunityInvitationCreatedEvent extends ApplicationEvent {
    private final UUID invitationId;
    private final UUID communityId;
    private final Long inviteeId;

    public CommunityInvitationCreatedEvent(Object source, UUID invitationId, UUID communityId, Long inviteeId) {
        super(source);
        this.invitationId = invitationId;
        this.communityId = communityId;
        this.inviteeId = inviteeId;
    }
}
