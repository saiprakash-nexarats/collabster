package com.collabsphere_first.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveProjectFileRequest {
    @NotBlank(message = "Path is required")
    private String path;
    private String content;
    private String fileType;
    private UUID storageObjectId;
}
