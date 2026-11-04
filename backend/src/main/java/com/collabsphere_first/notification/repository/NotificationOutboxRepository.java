package com.collabsphere_first.notification.repository;

import com.collabsphere_first.notification.entity.NotificationOutbox;
import com.collabsphere_first.notification.enums.NotificationChannel;
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
public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, UUID> {

    Optional<NotificationOutbox> findByNotificationIdAndChannel(UUID notificationId, NotificationChannel channel);

    List<NotificationOutbox> findByIdIn(List<UUID> ids);

    @Query(value = "SELECT id FROM notification_outbox " +
                   "WHERE (status = 'PENDING' AND next_attempt_at <= :now) " +
                   "   OR (status = 'IN_PROGRESS' AND locked_at < :staleLockCutoff) " +
                   "ORDER BY next_attempt_at ASC " +
                   "LIMIT :limit " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<UUID> findClaimableJobIds(@Param("now") Instant now,
                                   @Param("staleLockCutoff") Instant staleLockCutoff,
                                   @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationOutbox o SET o.status = com.collabsphere_first.notification.enums.OutboxStatus.IN_PROGRESS, " +
           "o.lockedAt = :now, o.lockedBy = :nodeId, o.attemptCount = o.attemptCount + 1, o.updatedAt = :now " +
           "WHERE o.id IN :ids")
    int claimJobs(@Param("ids") List<UUID> ids,
                  @Param("nodeId") String nodeId,
                  @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NotificationOutbox o WHERE (o.status = com.collabsphere_first.notification.enums.OutboxStatus.SENT " +
           "OR o.status = com.collabsphere_first.notification.enums.OutboxStatus.SKIPPED) AND o.updatedAt < :cutoff")
    int deleteCompletedOlderThan(@Param("cutoff") Instant cutoff);
}
