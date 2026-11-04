package com.collabsphere_first.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadInitiationRequest {

    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "ContentType is required")
    private String contentType;

    @Positive(message = "ContentLength must be greater than zero")
    private Long contentLength;

    private UUID workspaceId;
}

