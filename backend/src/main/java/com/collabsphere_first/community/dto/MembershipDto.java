package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MembershipDto {
    private UUID id;
    private UUID communityId;
    private Long userId;
    private MembershipRole role;
    private MembershipStatus status;
    private int contributionScore;
    private String notificationPref;
    private Instant joinedAt;
}
