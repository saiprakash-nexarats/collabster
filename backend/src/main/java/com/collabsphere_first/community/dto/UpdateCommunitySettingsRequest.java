package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.MembershipRole;
import lombok.Data;

@Data
public class UpdateCommunitySettingsRequest {
    private Boolean memberPostsEnabled; private Boolean commentsEnabled; private Boolean channelsEnabled;
    private Boolean messagingEnabled; private Boolean pollsEnabled; private Boolean eventsEnabled;
    private Boolean memberInvitesEnabled; private Boolean aiModerationEnabled;
    private MembershipRole channelCreationRole; private MembershipRole messageSendRole; private MembershipRole pollCreationRole;
}
