package com.collabsphere_first.report.dto;

import com.collabsphere_first.report.enums.PlatformModerationAction;
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
public class ResolvePlatformReportRequest {

    @NotNull(message = "action is required")
    private PlatformModerationAction action;

    @Size(max = 2000, message = "resolutionNotes cannot exceed 2000 characters")
    private String resolutionNotes;
}
