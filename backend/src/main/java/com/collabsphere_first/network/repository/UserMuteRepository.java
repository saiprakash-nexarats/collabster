package com.collabsphere_first.network.repository;

import com.collabsphere_first.network.entity.UserMute;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMuteRepository extends JpaRepository<UserMute, UUID> {

    boolean existsByMuterIdAndMutedId(Long muterId, Long mutedId);

    Optional<UserMute> findByMuterIdAndMutedId(Long muterId, Long mutedId);

    void deleteByMuterIdAndMutedId(Long muterId, Long mutedId);

    long countByMuterId(Long muterId);

    @Query("SELECT m FROM UserMute m JOIN FETCH m.muted WHERE m.muter.id = :muterId ORDER BY m.createdAt DESC, m.id DESC")
    List<UserMute> findInitialMuted(@Param("muterId") Long muterId, Pageable pageable);

    @Query("SELECT m FROM UserMute m JOIN FETCH m.muted WHERE m.muter.id = :muterId " +
           "AND (m.createdAt < :cursorCreatedAt OR (m.createdAt = :cursorCreatedAt AND m.id < :cursorId)) " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<UserMute> findMutedAfterCursor(
            @Param("muterId") Long muterId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT m.muted.id FROM UserMute m WHERE m.muter.id = :muterId")
    java.util.Set<Long> findMutedUserIds(@Param("muterId") Long muterId);

    @Query("SELECT m.muted.id FROM UserMute m WHERE m.muter.id = :muterId AND m.muted.id IN :candidateIds")
    java.util.Set<Long> findMutedUserIdsAmong(@Param("muterId") Long muterId, @Param("candidateIds") java.util.Collection<Long> candidateIds);
}
