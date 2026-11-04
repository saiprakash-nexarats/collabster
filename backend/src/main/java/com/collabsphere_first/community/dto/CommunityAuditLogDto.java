package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CommunityAuditLogDto {
    UUID id;
    UUID communityId;
    Long actorId;
    String action;
    String targetType;
    String targetId;
    String metadata;
    Instant createdAt;
}
