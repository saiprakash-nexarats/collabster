package com.collabsphere_first.interaction.repository;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.entity.ContentSave;
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
public interface ContentSaveRepository extends JpaRepository<ContentSave, UUID> {

    boolean existsByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    Optional<ContentSave> findByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    long countByContentTypeAndContentId(ContentType contentType, UUID contentId);

    void deleteByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    @Query("SELECT s FROM ContentSave s WHERE s.user.id = :userId ORDER BY s.createdAt DESC, s.id DESC")
    List<ContentSave> findInitialSavedPosts(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM ContentSave s WHERE s.user.id = :userId " +
           "AND (s.createdAt < :cursorCreatedAt OR (s.createdAt = :cursorCreatedAt AND s.id < :cursorId)) " +
           "ORDER BY s.createdAt DESC, s.id DESC")
    List<ContentSave> findSavedPostsAfterCursor(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT s.contentType, s.contentId FROM ContentSave s WHERE s.user.id = :userId AND s.contentType = :contentType AND s.contentId IN :contentIds")
    java.util.List<Object[]> findSavedContent(
            @Param("userId") Long userId,
            @Param("contentType") ContentType contentType,
            @Param("contentIds") java.util.Collection<UUID> contentIds
    );
}
