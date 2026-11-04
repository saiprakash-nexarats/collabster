package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResolveReportRequest {

    @NotNull(message = "Resolution status is required")
    private ReportStatus status;

    private String resolutionNotes;
}
