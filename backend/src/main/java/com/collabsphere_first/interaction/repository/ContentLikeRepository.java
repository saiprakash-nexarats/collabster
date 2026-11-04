package com.collabsphere_first.interaction.repository;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.entity.ContentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentLikeRepository extends JpaRepository<ContentLike, UUID> {

    boolean existsByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    Optional<ContentLike> findByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    long countByContentTypeAndContentId(ContentType contentType, UUID contentId);

    void deleteByUserIdAndContentTypeAndContentId(Long userId, ContentType contentType, UUID contentId);

    @Query("SELECT l.contentType, l.contentId, COUNT(l) FROM ContentLike l WHERE l.contentType = :contentType AND l.contentId IN :contentIds GROUP BY l.contentType, l.contentId")
    List<Object[]> countLikesByContentTypeAndIds(
            @Param("contentType") ContentType contentType,
            @Param("contentIds") Collection<UUID> contentIds
    );

    @Query("SELECT l.contentType, l.contentId FROM ContentLike l WHERE l.user.id = :userId AND l.contentType = :contentType AND l.contentId IN :contentIds")
    List<Object[]> findLikedContent(
            @Param("userId") Long userId,
            @Param("contentType") ContentType contentType,
            @Param("contentIds") Collection<UUID> contentIds
    );
}
