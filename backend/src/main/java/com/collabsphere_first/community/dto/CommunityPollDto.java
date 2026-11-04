package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class CommunityPollDto {
    UUID id;
    UUID communityId;
    Long createdBy;
    String question;
    boolean allowMultiple;
    boolean closed;
    Instant closesAt;
    Instant createdAt;
    List<CommunityPollOptionDto> options;
}
