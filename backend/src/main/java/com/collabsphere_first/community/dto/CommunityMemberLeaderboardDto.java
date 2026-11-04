package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommunityMemberLeaderboardDto {
    Long userId;
    int contributionScore;
}
