package com.collabsphere_first.notification.repository;

import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
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
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByEventKey(String eventKey);

    boolean existsByEventKey(String eventKey);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.archivedAt IS NULL " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findInitialByRecipient(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.archivedAt IS NULL " +
           "AND ((n.createdAt < :cursorCreatedAt) OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)) " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findAfterCursorByRecipient(@Param("userId") Long userId,
                                                  @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                                  @Param("cursorId") UUID cursorId,
                                                  Pageable pageable);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.category = :category AND n.archivedAt IS NULL " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findInitialByRecipientAndCategory(@Param("userId") Long userId,
                                                         @Param("category") NotificationCategory category,
                                                         Pageable pageable);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.category = :category AND n.archivedAt IS NULL " +
           "AND ((n.createdAt < :cursorCreatedAt) OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)) " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findAfterCursorByRecipientAndCategory(@Param("userId") Long userId,
                                                             @Param("category") NotificationCategory category,
                                                             @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                                             @Param("cursorId") UUID cursorId,
                                                             Pageable pageable);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findInitialUnreadByRecipient(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n " +
           "WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL " +
           "AND ((n.createdAt < :cursorCreatedAt) OR (n.createdAt = :cursorCreatedAt AND n.id < :cursorId)) " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findAfterCursorUnreadByRecipient(@Param("userId") Long userId,
                                                        @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                                        @Param("cursorId") UUID cursorId,
                                                        Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL")
    long countUnreadByRecipient(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now, n.seenAt = COALESCE(n.seenAt, :now) " +
           "WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL " +
           "AND ((n.createdAt < :upToCreatedAt) OR (n.createdAt = :upToCreatedAt AND n.id <= :upToId))")
    int markAllReadUpToTuple(@Param("userId") Long userId,
                             @Param("upToCreatedAt") Instant upToCreatedAt,
                             @Param("upToId") UUID upToId,
                             @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now, n.seenAt = COALESCE(n.seenAt, :now) " +
           "WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL")
    int markAllRead(@Param("userId") Long userId,
                    @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.readAt = :now, n.seenAt = COALESCE(n.seenAt, :now) " +
           "WHERE n.recipientUserId = :userId AND n.readAt IS NULL AND n.archivedAt IS NULL AND n.createdAt <= :upToTimestamp")
    int markAllReadUpTo(@Param("userId") Long userId,
                        @Param("upToTimestamp") Instant upToTimestamp,
                        @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.archivedAt IS NOT NULL AND n.archivedAt < :cutoff " +
           "AND NOT EXISTS (SELECT 1 FROM NotificationOutbox o WHERE o.notificationId = n.id " +
           "AND (o.status = com.collabsphere_first.notification.enums.OutboxStatus.PENDING " +
           "  OR o.status = com.collabsphere_first.notification.enums.OutboxStatus.IN_PROGRESS))")
    int deleteArchivedOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.readAt IS NOT NULL AND n.readAt < :cutoff " +
           "AND NOT EXISTS (SELECT 1 FROM NotificationOutbox o WHERE o.notificationId = n.id " +
           "AND (o.status = com.collabsphere_first.notification.enums.OutboxStatus.PENDING " +
           "  OR o.status = com.collabsphere_first.notification.enums.OutboxStatus.IN_PROGRESS))")
    int deleteReadOlderThan(@Param("cutoff") Instant cutoff);
}
