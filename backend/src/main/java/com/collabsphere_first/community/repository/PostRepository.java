package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Optional<Post> findByIdAndCommunity_Id(UUID id, UUID communityId);
    Page<Post> findByCommunityIdAndModerationStatusOrderByPinnedDescCreatedAtDesc(
            UUID communityId, ModerationStatus moderationStatus, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.community.id = :communityId AND (p.moderationStatus = :status OR p.authorId = :authorId) ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findVisibleOrAuthorPosts(@Param("communityId") UUID communityId,
                                        @Param("status") ModerationStatus status,
                                        @Param("authorId") Long authorId,
                                        Pageable pageable);

    long countByCommunityIdAndCreatedAtAfter(UUID communityId, Instant since);
    long countByCommunityId(UUID communityId);
    long countByCommunityIdAndModerationStatus(UUID communityId, ModerationStatus status);
    long countByCommunityIdAndCreatedAtBetween(UUID communityId, Instant from, Instant to);
    List<Post> findByCommunityIdAndCreatedAtBetween(UUID communityId, Instant from, Instant to);
    @Query("SELECT p FROM Post p JOIN FETCH p.community WHERE p.id IN :postIds")
    List<Post> findByIdsWithCommunity(@Param("postIds") java.util.Collection<UUID> postIds);

    @Query("SELECT p FROM Post p JOIN FETCH p.community WHERE p.id = :id")
    Optional<Post> findByIdWithCommunity(@Param("id") UUID id);

    @Query("SELECT p FROM Post p WHERE p.community.id IN :communityIds AND p.moderationStatus = :status ORDER BY p.createdAt DESC")
    List<Post> findRecentApprovedPostsByCommunities(
            @Param("communityIds") java.util.Collection<UUID> communityIds,
            @Param("status") ModerationStatus status,
            Pageable pageable
    );
}
