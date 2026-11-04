package com.collabsphere_first.report.repository;

import com.collabsphere_first.report.entity.PlatformReport;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import com.collabsphere_first.report.enums.PlatformReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformReportRepository extends JpaRepository<PlatformReport, UUID> {

    Optional<PlatformReport> findByReporterUserIdAndTargetTypeAndTargetIdAndStatus(
            Long reporterUserId,
            PlatformReportTargetType targetType,
            String targetId,
            PlatformReportStatus status
    );

    Page<PlatformReport> findByStatusOrderByCreatedAtDesc(PlatformReportStatus status, Pageable pageable);

    Page<PlatformReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
