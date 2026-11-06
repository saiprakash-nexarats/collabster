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
public class ProjectMilestoneDto {
    private UUID id;
    private UUID projectId;
    private String title;
    private Instant targetDate;
    private String status;
    private Long createdBy;
    private Instant createdAt;
}
