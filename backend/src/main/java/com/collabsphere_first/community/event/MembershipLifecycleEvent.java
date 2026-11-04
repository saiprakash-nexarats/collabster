package com.collabsphere_first.community.event;

import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class MembershipLifecycleEvent extends ApplicationEvent {
    public enum Type {
        JOIN_REQUESTED,
        JOIN_APPROVED,
        JOIN_REJECTED,
        MEMBER_LEFT,
        MEMBER_BANNED,
        MEMBER_UNBANNED,
        MEMBER_MUTED,
        MEMBER_UNMUTED,
        MEMBER_SUSPENDED,
        ROLE_CHANGED,
        OWNERSHIP_TRANSFERRED
    }

    private final UUID communityId;
    private final Long userId;
    private final Long actorId;
    private final Type type;
    private final MembershipRole oldRole;
    private final MembershipRole newRole;
    private final MembershipStatus oldStatus;
    private final MembershipStatus newStatus;

    public MembershipLifecycleEvent(
            Object source,
            UUID communityId,
            Long userId,
            Long actorId,
            Type type,
            MembershipRole oldRole,
            MembershipRole newRole,
            MembershipStatus oldStatus,
            MembershipStatus newStatus
    ) {
        super(source);
        this.communityId = communityId;
        this.userId = userId;
        this.actorId = actorId;
        this.type = type;
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
