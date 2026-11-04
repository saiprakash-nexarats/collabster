package com.collabsphere_first.report.service;

import com.collabsphere_first.report.dto.CreatePlatformReportRequest;
import com.collabsphere_first.report.dto.PlatformReportResponse;
import com.collabsphere_first.report.dto.ResolvePlatformReportRequest;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PlatformReportService {

    PlatformReportResponse fileReport(CreatePlatformReportRequest request, Long reporterUserId);

    Page<PlatformReportResponse> getReportQueue(PlatformReportStatus status, Pageable pageable);

    PlatformReportResponse resolveReport(UUID reportId, ResolvePlatformReportRequest request, Long adminId);
}
