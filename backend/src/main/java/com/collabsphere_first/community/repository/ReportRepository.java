package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Report;
import com.collabsphere_first.community.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            UUID communityId, ReportStatus status, Pageable pageable);
    
    Page<Report> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
    long countByCommunityId(UUID communityId);
    long countByCommunityIdAndStatus(UUID communityId, ReportStatus status);
}
