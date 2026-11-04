package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Resource;
import com.collabsphere_first.community.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    Optional<Resource> findByIdAndCommunity_Id(UUID id, UUID communityId);
    Page<Resource> findByCommunityIdAndModerationStatusOrderByCreatedAtDesc(
            UUID communityId, ModerationStatus moderationStatus, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.community.id = :communityId AND (r.moderationStatus = :status OR r.uploadedBy = :authorId) ORDER BY r.createdAt DESC")
    Page<Resource> findVisibleOrAuthorResources(@Param("communityId") UUID communityId,
                                                @Param("status") ModerationStatus status,
                                                @Param("authorId") Long authorId,
                                                Pageable pageable);
    long countByCommunity_Id(UUID communityId);
    long countByCommunity_IdAndModerationStatus(UUID communityId, ModerationStatus status);
    long countByCommunity_IdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    java.util.List<Resource> findByCommunity_IdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
}
