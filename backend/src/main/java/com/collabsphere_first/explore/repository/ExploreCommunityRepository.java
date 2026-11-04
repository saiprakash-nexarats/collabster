package com.collabsphere_first.explore.repository;

import com.collabsphere_first.community.entity.Community;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExploreCommunityRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Searches communities by name, description, and tags.
     * Only surfaces PUBLIC communities or non-public communities where the viewer is an active member.
     */
    @SuppressWarnings("unchecked")
    public List<Community> searchCommunities(String query, Set<UUID> joinedCommunityIds, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.*
            FROM communities c
            WHERE c.community_status = :activeStatus
              AND c.archived_at IS NULL
              AND (
                c.visibility = :publicVisibility
            """);

        if (joinedCommunityIds != null && !joinedCommunityIds.isEmpty()) {
            sql.append(" OR c.id IN (:joinedIds) ");
        }

        sql.append("""
              )
              AND (
                lower(c.name) LIKE concat(lower(:q), '%')
                OR to_tsvector('english', coalesce(c.name, '') || ' ' || coalesce(c.description, '')) @@ plainto_tsquery('english', :q)
                OR c.tags @> ARRAY[CAST(:q AS text)]
              )
            ORDER BY
              CASE
                WHEN lower(c.name) = lower(:q) THEN 0
                WHEN lower(c.name) LIKE lower(concat(:q, '%')) THEN 1
                ELSE 2
              END,
              c.activity_score DESC, c.member_count DESC, c.created_at DESC, c.id DESC
            LIMIT :lim
            """);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), Community.class);
        nativeQuery.setParameter("q", query);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("activeStatus", com.collabsphere_first.community.enums.CommunityStatus.ACTIVE.name());
        nativeQuery.setParameter("publicVisibility", com.collabsphere_first.community.enums.Visibility.PUBLIC.name());
        if (joinedCommunityIds != null && !joinedCommunityIds.isEmpty()) {
            nativeQuery.setParameter("joinedIds", joinedCommunityIds);
        }

        return nativeQuery.getResultList();
    }

    /**
     * Discovers active public communities sorted by activity score and member count.
     */
    @SuppressWarnings("unchecked")
    public List<Community> discoverCommunities(Set<UUID> excludedCommunityIds, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.*
            FROM communities c
            WHERE c.community_status = :activeStatus
              AND c.archived_at IS NULL
              AND c.visibility = :publicVisibility
            """);

        if (excludedCommunityIds != null && !excludedCommunityIds.isEmpty()) {
            sql.append(" AND c.id NOT IN (:excludedIds) ");
        }

        sql.append("""
            ORDER BY c.activity_score DESC, c.member_count DESC, c.created_at DESC, c.id DESC
            LIMIT :lim
            """);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), Community.class);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("activeStatus", com.collabsphere_first.community.enums.CommunityStatus.ACTIVE.name());
        nativeQuery.setParameter("publicVisibility", com.collabsphere_first.community.enums.Visibility.PUBLIC.name());
        if (excludedCommunityIds != null && !excludedCommunityIds.isEmpty()) {
            nativeQuery.setParameter("excludedIds", excludedCommunityIds);
        }

        return nativeQuery.getResultList();
    }

    /**
     * Batch check which community IDs the user has active membership in.
     */
    public Set<UUID> findJoinedCommunityIds(Long userId, Collection<UUID> communityIds) {
        if (userId == null || communityIds == null || communityIds.isEmpty()) {
            return Collections.emptySet();
        }
        String jpql = "SELECT m.community.id FROM Membership m WHERE m.userId = :userId AND m.status = 'ACTIVE' AND m.community.id IN (:ids)";
        TypedQuery<UUID> query = entityManager.createQuery(jpql, UUID.class);
        query.setParameter("userId", userId);
        query.setParameter("ids", communityIds);
        return new HashSet<>(query.getResultList());
    }

    /**
     * Finds all active community IDs the user is a member of.
     */
    public Set<UUID> findAllActiveCommunityIdsForUser(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        String jpql = "SELECT m.community.id FROM Membership m WHERE m.userId = :userId AND m.status = 'ACTIVE'";
        TypedQuery<UUID> query = entityManager.createQuery(jpql, UUID.class);
        query.setParameter("userId", userId);
        return new HashSet<>(query.getResultList());
    }
}
