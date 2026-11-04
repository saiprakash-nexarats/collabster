package com.collabsphere_first.community.dto;
import com.collabsphere_first.community.enums.EventRsvpStatus;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;
@Value @Builder
public class CommunityEventDto {
    UUID id; UUID communityId; Long createdBy; String title; String description; Instant startsAt; Instant endsAt;
    String location; Instant createdAt; long goingCount; long maybeCount; EventRsvpStatus myRsvp;
}
