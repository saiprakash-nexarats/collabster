package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMembershipRequest {
    private MembershipRole role;
    private MembershipStatus status;
    private String notificationPref;
}
