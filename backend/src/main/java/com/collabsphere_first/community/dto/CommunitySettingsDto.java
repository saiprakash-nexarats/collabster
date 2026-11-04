package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.MembershipRole;
import lombok.Builder;
import lombok.Value;
import java.util.UUID;

@Value @Builder
public class CommunitySettingsDto {
    UUID communityId;
    boolean memberPostsEnabled; boolean commentsEnabled; boolean channelsEnabled; boolean messagingEnabled;
    boolean pollsEnabled; boolean eventsEnabled; boolean memberInvitesEnabled; boolean aiModerationEnabled;
    MembershipRole channelCreationRole; MembershipRole messageSendRole; MembershipRole pollCreationRole;
}
