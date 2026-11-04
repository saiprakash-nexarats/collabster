package com.collabsphere_first.community.event;

import com.collabsphere_first.community.enums.MembershipRole;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class MemberJoinedEvent extends ApplicationEvent {
    private final UUID communityId;
    private final Long userId;
    private final MembershipRole role;

    public MemberJoinedEvent(Object source, UUID communityId, Long userId, MembershipRole role) {
        super(source);
        this.communityId = communityId;
        this.userId = userId;
        this.role = role;
    }
}
