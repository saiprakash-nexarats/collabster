package com.collabsphere_first.report.repository;

import com.collabsphere_first.report.entity.PlatformAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, UUID> {

    Page<PlatformAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
