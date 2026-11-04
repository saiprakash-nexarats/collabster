package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityPoll;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommunityPollRepository extends JpaRepository<CommunityPoll, UUID> {
    Page<CommunityPoll> findByCommunityIdOrderByCreatedAtDesc(UUID communityId, Pageable pageable);
    long countByCommunityId(UUID communityId);
    long countByCommunity_IdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    java.util.List<CommunityPoll> findByCommunity_IdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from CommunityPoll p where p.id = :id")
    Optional<CommunityPoll> findByIdForUpdate(@Param("id") UUID id);
}
