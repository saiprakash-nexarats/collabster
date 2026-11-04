package com.collabsphere_first.recommendation.feedback.repository;

import com.collabsphere_first.recommendation.feedback.dto.SafeDeadLetterSummary;
import com.collabsphere_first.recommendation.feedback.entity.RecommendationFeedbackOutbox;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus;
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
public interface RecommendationFeedbackOutboxRepository extends JpaRepository<RecommendationFeedbackOutbox, UUID> {

    Optional<RecommendationFeedbackOutbox> findByEventId(UUID eventId);

    List<RecommendationFeedbackOutbox> findByIdIn(List<UUID> ids);

    @Query(value = "SELECT id FROM recommendation_feedback_outbox " +
                   "WHERE (status IN ('PENDING', 'RETRY') AND next_attempt_at <= :now) " +
                   "   OR (status = 'IN_PROGRESS' AND locked_at < :staleLockCutoff) " +
                   "ORDER BY next_attempt_at ASC, id ASC " +
                   "LIMIT :limit " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<UUID> findClaimableJobIds(@Param("now") Instant now,
                                   @Param("staleLockCutoff") Instant staleLockCutoff,
                                   @Param("limit") int limit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RecommendationFeedbackOutbox o SET o.status = com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus.IN_PROGRESS, " +
           "o.lockedAt = :now, o.lockOwner = :nodeId, o.attemptCount = o.attemptCount + 1, o.updatedAt = :now " +
           "WHERE o.id IN :ids")
    int claimJobs(@Param("ids") List<UUID> ids,
                  @Param("nodeId") String nodeId,
                  @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RecommendationFeedbackOutbox o WHERE o.status = com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus.DELIVERED " +
           "AND o.updatedAt < :cutoff")
    int deleteCompletedOlderThan(@Param("cutoff") Instant cutoff);

    long countByStatus(FeedbackOutboxStatus status);

    @Query("SELECT MIN(o.createdAt) FROM RecommendationFeedbackOutbox o " +
           "WHERE o.status IN (com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus.PENDING, " +
           "                   com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus.RETRY)")
    Optional<Instant> findOldestPendingCreatedAt();

    @Query("SELECT new com.collabsphere_first.recommendation.feedback.dto.SafeDeadLetterSummary(" +
           "o.eventId, o.eventType, o.lastErrorCode, o.attemptCount, o.occurredAt) " +
           "FROM RecommendationFeedbackOutbox o " +
           "WHERE o.status = com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus.DEAD_LETTER " +
           "ORDER BY o.updatedAt DESC")
    List<SafeDeadLetterSummary> findDeadLetterSummaries(Pageable pageable);
}
