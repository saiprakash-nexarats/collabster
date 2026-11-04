package com.collabsphere_first.network.repository;

import com.collabsphere_first.network.entity.UserBlock;
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
public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT COUNT(b) > 0 FROM UserBlock b WHERE " +
           "(b.blocker.id = :userA AND b.blocked.id = :userB) OR " +
           "(b.blocker.id = :userB AND b.blocked.id = :userA)")
    boolean isBlockedEitherDirection(@Param("userA") Long userA, @Param("userB") Long userB);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT b FROM UserBlock b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId ORDER BY b.createdAt DESC, b.id DESC")
    List<UserBlock> findInitialBlocked(@Param("blockerId") Long blockerId, Pageable pageable);

    @Query("SELECT b FROM UserBlock b JOIN FETCH b.blocked WHERE b.blocker.id = :blockerId " +
           "AND (b.createdAt < :cursorCreatedAt OR (b.createdAt = :cursorCreatedAt AND b.id < :cursorId)) " +
           "ORDER BY b.createdAt DESC, b.id DESC")
    List<UserBlock> findBlockedAfterCursor(
            @Param("blockerId") Long blockerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END " +
           "FROM UserBlock b WHERE b.blocker.id = :userId OR b.blocked.id = :userId")
    java.util.Set<Long> findBlockedOrBlockingUserIds(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END " +
           "FROM UserBlock b WHERE ((b.blocker.id = :userId AND b.blocked.id IN :candidateIds) " +
           "OR (b.blocked.id = :userId AND b.blocker.id IN :candidateIds))")
    java.util.Set<Long> findBlockedOrBlockingUserIdsAmong(@Param("userId") Long userId, @Param("candidateIds") java.util.Collection<Long> candidateIds);
}
