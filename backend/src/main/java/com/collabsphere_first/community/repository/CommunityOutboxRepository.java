package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityOutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CommunityOutboxRepository extends JpaRepository<CommunityOutboxEvent, UUID> {
    Page<CommunityOutboxEvent> findByPublishedAtIsNullAndFailedAtIsNullAndAvailableAtBeforeOrderByCreatedAtAsc(Instant now, Pageable pageable);
    Page<CommunityOutboxEvent>
    findByPublishedAtIsNullAndFailedAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            Instant now,
            Pageable pageable
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from CommunityOutboxEvent e where e.id = :id")
    Optional<CommunityOutboxEvent> findByIdForUpdate(@Param("id") UUID id);
}
