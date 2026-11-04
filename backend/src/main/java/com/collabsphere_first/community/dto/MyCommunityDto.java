package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.Visibility;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class MyCommunityDto {
    UUID communityId;
    String name;
    String slug;
    String description;
    Visibility visibility;
    MembershipRole role;
    MembershipStatus membershipStatus;
    int memberCount;
    BigDecimal activityScore;
}
