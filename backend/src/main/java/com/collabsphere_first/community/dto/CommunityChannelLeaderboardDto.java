package com.collabsphere_first.community.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class CommunityChannelLeaderboardDto {
    UUID channelId;
    String channelName;
    long messageCount;
}
