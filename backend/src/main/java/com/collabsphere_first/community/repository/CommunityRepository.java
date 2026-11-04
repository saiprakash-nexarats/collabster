package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.CommunityType;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.enums.CommunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {
    Optional<Community> findBySlug(String slug);
    boolean existsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Community c where c.id = :id")
    Optional<Community> findByIdForUpdate(@Param("id") UUID id);

    Page<Community> findByTypeAndVisibility(CommunityType type, Visibility visibility, Pageable pageable);
    Page<Community> findByVisibility(Visibility visibility, Pageable pageable);
    Page<Community> findByType(CommunityType type, Pageable pageable);

    @Query(value = """
            SELECT * FROM communities c
            WHERE c.visibility = 'PUBLIC' AND c.archived_at IS NULL AND c.community_status = 'ACTIVE'
              AND (:type IS NULL OR c.type = :type)
              AND (:category IS NULL OR c.category = :category)
              AND (:q IS NULL OR lower(c.name) LIKE lower(concat('%', :q, '%'))
                   OR lower(coalesce(c.description, '')) LIKE lower(concat('%', :q, '%')))
              AND (:tag IS NULL OR c.tags @> ARRAY[CAST(:tag AS text)])
            ORDER BY c.activity_score DESC, c.member_count DESC, c.created_at DESC
            """, countQuery = """
            SELECT count(*) FROM communities c
            WHERE c.visibility = 'PUBLIC' AND c.archived_at IS NULL AND c.community_status = 'ACTIVE'
              AND (:type IS NULL OR c.type = :type)
              AND (:category IS NULL OR c.category = :category)
              AND (:q IS NULL OR lower(c.name) LIKE lower(concat('%', :q, '%'))
                   OR lower(coalesce(c.description, '')) LIKE lower(concat('%', :q, '%')))
              AND (:tag IS NULL OR c.tags @> ARRAY[CAST(:tag AS text)])
            """, nativeQuery = true)
    Page<Community> discoverPublic(@Param("q") String query, @Param("type") String type,
                                   @Param("category") String category,
                                   @Param("tag") String tag, Pageable pageable);

    Page<Community> findByVisibilityAndArchivedAtIsNull(Visibility visibility, Pageable pageable);
    Page<Community> findByVisibilityAndCommunityStatus(Visibility visibility, CommunityStatus status, Pageable pageable);
    Page<Community> findByCommunityStatusAndDeletionRequestedAtBefore(CommunityStatus status, java.time.Instant cutoff, Pageable pageable);
}
