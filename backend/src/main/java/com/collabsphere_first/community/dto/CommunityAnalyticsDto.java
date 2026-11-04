package com.collabsphere_first.community.dto;
import lombok.Builder;
import lombok.Value;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
@Value @Builder
public class CommunityAnalyticsDto {
    UUID communityId; Instant from; Instant to; long newMembers; long posts; long comments;
    long resources; long messages; long polls; long events; long eventRsvps;
    List<CommunityAnalyticsDayDto> daily;
    List<CommunityMemberLeaderboardDto> topMembers;
    List<CommunityChannelLeaderboardDto> topChannels;
}
