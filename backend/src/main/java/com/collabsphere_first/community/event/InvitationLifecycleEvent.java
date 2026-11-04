package com.collabsphere_first.community.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class InvitationLifecycleEvent extends ApplicationEvent {
    public enum Type {
        INVITATION_ACCEPTED,
        INVITATION_DECLINED,
        INVITATION_REVOKED,
        INVITATION_EXPIRED
    }

    private final UUID invitationId;
    private final UUID communityId;
    private final Long inviterId;
    private final Long inviteeId;
    private final Type type;

    public InvitationLifecycleEvent(
            Object source,
            UUID invitationId,
            UUID communityId,
            Long inviterId,
            Long inviteeId,
            Type type
    ) {
        super(source);
        this.invitationId = invitationId;
        this.communityId = communityId;
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
        this.type = type;
    }
}
