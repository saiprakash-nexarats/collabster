package com.collabsphere_first.post.repository;

import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
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
public interface PlatformPostRepository extends JpaRepository<PlatformPost, UUID> {

    /**
     * Finds an active (or specific status) post by ID.
     */
    Optional<PlatformPost> findByIdAndStatus(UUID id, PostStatus status);

    /**
     * Keyset / cursor query: initial page of author posts.
     */
    @Query("SELECT p FROM PlatformPost p WHERE p.author.id = :authorId AND p.status = :status ORDER BY p.createdAt DESC, p.id DESC")
    List<PlatformPost> findInitialAuthorPosts(
            @Param("authorId") Long authorId,
            @Param("status") PostStatus status,
            Pageable pageable
    );

    /**
     * Keyset / cursor query: subsequent page of author posts based on opaque cursor (createdAt, id).
     */
    @Query("SELECT p FROM PlatformPost p WHERE p.author.id = :authorId AND p.status = :status " +
            "AND (p.createdAt < :cursorCreatedAt OR (p.createdAt = :cursorCreatedAt AND p.id < :cursorId)) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<PlatformPost> findAuthorPostsAfterCursor(
            @Param("authorId") Long authorId,
            @Param("status") PostStatus status,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    /**
     * Count active posts by an author.
     */
    long countByAuthorIdAndStatus(Long authorId, PostStatus status);

    /**
     * Future Feed / Explore helper query: fetch eligible public posts.
     */
    @Query("SELECT p FROM PlatformPost p WHERE p.status = :status AND p.visibility = :visibility ORDER BY p.createdAt DESC")
    List<PlatformPost> findPublicPosts(
            @Param("status") PostStatus status,
            @Param("visibility") PostVisibility visibility,
            Pageable pageable
    );

    @Query("SELECT p FROM PlatformPost p WHERE p.author.id IN :authorIds " +
           "AND p.status = :status AND p.visibility IN :visibilities ORDER BY p.createdAt DESC")
    List<PlatformPost> findCandidatePostsByAuthors(
            @Param("authorIds") java.util.Collection<Long> authorIds,
            @Param("status") PostStatus status,
            @Param("visibilities") java.util.Collection<PostVisibility> visibilities,
            Pageable pageable
    );

    @Query("SELECT p FROM PlatformPost p WHERE p.status = 'ACTIVE' AND p.visibility = 'PUBLIC' " +
           "AND p.author.id NOT IN :excludedAuthorIds ORDER BY p.createdAt DESC")
    List<PlatformPost> findCandidatePublicPostsExcludingAuthors(
            @Param("excludedAuthorIds") java.util.Collection<Long> excludedAuthorIds,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM PlatformPost p LEFT JOIN FETCH p.media WHERE p.id IN :postIds")
    List<PlatformPost> findByIdsWithMedia(@Param("postIds") java.util.Collection<UUID> postIds);
}
