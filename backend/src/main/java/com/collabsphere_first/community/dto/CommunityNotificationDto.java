package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value @Builder
public class CommunityNotificationDto {
    UUID id; Long userId; UUID communityId; String type; String title; String body;
    String targetType; String targetId; Instant readAt; Instant createdAt;
}
