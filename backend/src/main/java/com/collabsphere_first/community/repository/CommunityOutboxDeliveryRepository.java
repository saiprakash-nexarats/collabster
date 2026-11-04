package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityOutboxDelivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface CommunityOutboxDeliveryRepository
        extends JpaRepository<CommunityOutboxDelivery, UUID> {

    Page<CommunityOutboxDelivery>
    findByDeliveredAtIsNullAndFailedAtIsNullAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
            Instant now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from CommunityOutboxDelivery d where d.id = :id")
    java.util.Optional<CommunityOutboxDelivery> findByIdForUpdate(@Param("id") UUID id);

    long countByEventIdAndDeliveredAtIsNull(UUID eventId);
}
