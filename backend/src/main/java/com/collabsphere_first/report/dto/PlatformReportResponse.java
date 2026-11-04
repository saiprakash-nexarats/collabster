package com.collabsphere_first.report.dto;

import com.collabsphere_first.report.enums.PlatformModerationAction;
import com.collabsphere_first.report.enums.PlatformReportReason;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import com.collabsphere_first.report.enums.PlatformReportTargetType;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlatformReportResponse {

    private UUID id;
    private PlatformReportTargetType targetType;
    private String targetId;
    private PlatformReportReason reason;
    private String details;
    private PlatformReportStatus status;
    private PlatformModerationAction resolution;
    private String resolutionNotes;
    private Instant createdAt;
    private Instant resolvedAt;
}
