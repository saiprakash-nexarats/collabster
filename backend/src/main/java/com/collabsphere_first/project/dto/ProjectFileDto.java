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
public class ProjectFileDto {
    private UUID id;
    private UUID projectId;
    private String path;
    private String content;
    private String fileType;
    private UUID storageObjectId;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
