package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.InvitationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class InvitationDto {
    UUID id;
    UUID communityId;
    String communityName;
    Long inviterId;
    Long inviteeId;
    InvitationStatus status;
    Instant createdAt;
    Instant expiresAt;
    Instant acceptedAt;
    Instant revokedAt;
    /** Present only in the response that creates the invitation. */
    String token;
}
