package com.collabsphere_first.community.event;

import com.collabsphere_first.community.enums.CommunityType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
public class CommunityCreatedEvent extends ApplicationEvent {
    private final UUID communityId;
    private final String name;
    private final CommunityType type;
    private final Long ownerId;

    public CommunityCreatedEvent(Object source, UUID communityId, String name, CommunityType type, Long ownerId) {
        super(source);
        this.communityId = communityId;
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
    }
}
