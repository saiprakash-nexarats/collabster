package com.collabsphere_first.community.event;

import java.util.UUID;

public record CommunityOutboxMessage(
        UUID eventId,
        UUID communityId,
        String eventType,
        String aggregateId,
        String payload
) {}

