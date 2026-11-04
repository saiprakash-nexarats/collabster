package com.collabsphere_first.report.dto;

import com.collabsphere_first.report.enums.PlatformReportReason;
import com.collabsphere_first.report.enums.PlatformReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlatformReportRequest {

    @NotNull(message = "targetType is required")
    private PlatformReportTargetType targetType;

    @NotBlank(message = "targetId is required")
    @Size(max = 100, message = "targetId cannot exceed 100 characters")
    private String targetId;

    @NotNull(message = "reason is required")
    private PlatformReportReason reason;

    @Size(max = 2000, message = "details cannot exceed 2000 characters")
    private String details;
}
