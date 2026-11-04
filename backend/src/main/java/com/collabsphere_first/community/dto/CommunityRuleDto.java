package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;

@Value @Builder
public class CommunityRuleDto {
    UUID id; UUID communityId; String title; String body; int position; int version; boolean active; Instant updatedAt;
}
