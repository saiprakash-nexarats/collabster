package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Comment;
import com.collabsphere_first.community.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findByIdAndCommunityId(UUID id, UUID communityId);
    Optional<Comment> findByIdAndPostId(UUID id, UUID postId);
    Page<Comment> findByPostIdAndModerationStatusOrderByCreatedAtAsc(
            UUID postId, ModerationStatus moderationStatus, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND (c.moderationStatus = :status OR c.authorId = :authorId) ORDER BY c.createdAt ASC")
    Page<Comment> findVisibleOrAuthorComments(@Param("postId") UUID postId,
                                              @Param("status") ModerationStatus status,
                                              @Param("authorId") Long authorId,
                                              Pageable pageable);

    List<Comment> findByParentCommentId(UUID parentCommentId);
    long countByCommunityId(UUID communityId);
    long countByCommunityIdAndModerationStatus(UUID communityId, ModerationStatus status);
    long countByCommunityIdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    List<Comment> findByCommunityIdAndCreatedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
}
