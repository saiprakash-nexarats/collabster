package com.collabsphere_first.community.dto;

import com.collabsphere_first.community.enums.ReportStatus;
import com.collabsphere_first.community.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportDto {
    private UUID id;
    private UUID communityId;
    private ReportTargetType targetType;
    private String targetId;
    private Long reporterId;
    private String reason;
    private ReportStatus status;
    private Long resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;
}
