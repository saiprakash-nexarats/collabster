package com.collabsphere_first.network.repository;

import com.collabsphere_first.network.entity.UserFollow;
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
public interface UserFollowRepository extends JpaRepository<UserFollow, UUID> {

    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    @Modifying
    @Query("DELETE FROM UserFollow f WHERE (f.follower.id = :userA AND f.following.id = :userB) OR (f.follower.id = :userB AND f.following.id = :userA)")
    void deleteAllFollowsBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT f FROM UserFollow f JOIN FETCH f.follower WHERE f.following.id = :followingId ORDER BY f.createdAt DESC, f.id DESC")
    List<UserFollow> findInitialFollowers(@Param("followingId") Long followingId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f JOIN FETCH f.follower WHERE f.following.id = :followingId " +
           "AND (f.createdAt < :cursorCreatedAt OR (f.createdAt = :cursorCreatedAt AND f.id < :cursorId)) " +
           "ORDER BY f.createdAt DESC, f.id DESC")
    List<UserFollow> findFollowersAfterCursor(
            @Param("followingId") Long followingId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT f FROM UserFollow f JOIN FETCH f.following WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC, f.id DESC")
    List<UserFollow> findInitialFollowing(@Param("followerId") Long followerId, Pageable pageable);

    @Query("SELECT f FROM UserFollow f JOIN FETCH f.following WHERE f.follower.id = :followerId " +
           "AND (f.createdAt < :cursorCreatedAt OR (f.createdAt = :cursorCreatedAt AND f.id < :cursorId)) " +
           "ORDER BY f.createdAt DESC, f.id DESC")
    List<UserFollow> findFollowingAfterCursor(
            @Param("followerId") Long followerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT f.following.id FROM UserFollow f WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    List<Long> findFollowingUserIds(@Param("followerId") Long followerId, Pageable pageable);

    @Query("SELECT f.following.id FROM UserFollow f WHERE f.follower.id = :followerId AND f.following.id IN :candidateIds")
    java.util.Set<Long> findFollowingUserIdsAmong(@Param("followerId") Long followerId, @Param("candidateIds") java.util.Collection<Long> candidateIds);
}
