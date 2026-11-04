package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CommunityNotificationRepository extends JpaRepository<CommunityNotification, UUID> {
    Page<CommunityNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<CommunityNotification> findByIdAndUserId(UUID id, Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);
    @Modifying
    @Query("update CommunityNotification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);
}
