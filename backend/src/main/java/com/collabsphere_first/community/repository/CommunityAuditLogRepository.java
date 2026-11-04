package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityAuditLogRepository extends JpaRepository<CommunityAuditLog, UUID> {
    Page<CommunityAuditLog> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
}
