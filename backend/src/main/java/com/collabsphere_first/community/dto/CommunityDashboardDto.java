package com.collabsphere_first.community.dto;
import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.util.UUID;
@Value @Builder
public class CommunityDashboardDto {
    UUID communityId; boolean moderationView;
    Long memberCount; Long activeMembers; Long pendingMembers; Long newMembers;
    Long postCount; Long commentCount; Long resourceCount; Long channelCount; Long messageCount;
    Long pollCount; Long reportCount; Long pendingReports; Long pendingModeration;
    BigDecimal activityScore;
}
