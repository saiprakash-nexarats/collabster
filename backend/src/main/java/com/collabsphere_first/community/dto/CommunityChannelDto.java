package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class CommunityChannelDto {
    UUID id;
    UUID communityId;
    String name;
    String description;
    boolean readOnly;
    Long createdBy;
    Instant createdAt;
}
