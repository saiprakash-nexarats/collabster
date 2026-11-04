package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value @Builder
public class CommunityRuleRevisionDto {
    UUID id; UUID ruleId; int version; String title; String body; boolean active; Long changedBy; Instant createdAt;
}
