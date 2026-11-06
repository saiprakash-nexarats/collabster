package com.collabsphere_first.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteProjectMemberRequest {
    private Long profileId;
    private Long userId;
    private String role;
}
