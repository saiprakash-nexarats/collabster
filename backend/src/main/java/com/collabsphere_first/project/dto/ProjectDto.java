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
public class ProjectDto {
    private UUID id;
    private Long ownerId;
    private String title;
    private String description;
    private String visibility;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}
