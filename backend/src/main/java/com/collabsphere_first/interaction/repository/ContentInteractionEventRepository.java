package com.collabsphere_first.interaction.repository;

import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.interaction.entity.ContentInteractionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ContentInteractionEventRepository extends JpaRepository<ContentInteractionEvent, UUID> {

    @Query("SELECT COUNT(e) > 0 FROM ContentInteractionEvent e WHERE e.user.id = :userId " +
           "AND e.contentType = :contentType AND e.contentId = :contentId " +
           "AND e.eventType = 'VIEW' AND e.createdAt >= :cutoff")
    boolean existsRecentView(
            @Param("userId") Long userId,
            @Param("contentType") ContentType contentType,
            @Param("contentId") UUID contentId,
            @Param("cutoff") Instant cutoff
    );

    @Query("SELECT DISTINCT e.contentType, e.contentId FROM ContentInteractionEvent e " +
           "WHERE e.user.id = :userId AND e.eventType IN ('HIDE', 'NOT_INTERESTED')")
    java.util.List<Object[]> findSuppressedContent(@Param("userId") Long userId);
}
