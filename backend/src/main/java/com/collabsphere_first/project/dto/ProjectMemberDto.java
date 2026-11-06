package com.collabsphere_first.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private UUID id;
    private UUID projectId;
    private Long userId;
    private String role;
    private String membershipState;
    private Long invitedBy;
    private Instant createdAt;
}
