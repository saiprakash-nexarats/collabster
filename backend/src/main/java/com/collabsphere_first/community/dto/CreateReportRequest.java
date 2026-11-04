package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Community ID is required")
    private UUID communityId;

    @NotNull(message = "Target type is required")
    private ReportTargetType targetType;

    @NotBlank(message = "Target ID is required")
    private String targetId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
