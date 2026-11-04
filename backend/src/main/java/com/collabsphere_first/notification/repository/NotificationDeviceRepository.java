package com.collabsphere_first.notification.repository;

import com.collabsphere_first.notification.entity.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {

    Optional<NotificationDevice> findByTokenHash(String tokenHash);

    List<NotificationDevice> findByUserIdAndEnabledTrue(Long userId);

    List<NotificationDevice> findByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationDevice d SET d.enabled = false, d.revokedAt = :now, d.updatedAt = :now WHERE d.tokenHash = :tokenHash")
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationDevice d SET d.enabled = false, d.revokedAt = :now, d.updatedAt = :now WHERE d.userId = :userId AND d.enabled = true")
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
