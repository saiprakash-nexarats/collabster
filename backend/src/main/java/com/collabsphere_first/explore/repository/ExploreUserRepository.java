package com.collabsphere_first.explore.repository;

import com.collabsphere_first.network.entity.UserConnection;
import com.collabsphere_first.network.enums.ConnectionStatus;
import com.collabsphere_first.user.entity.User;
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
public class ExploreUserRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Searches users by username, first name, and last name with block and privacy filtering.
     */
    @SuppressWarnings("unchecked")
    public List<User> searchUsers(String query, Long viewerId, Set<Long> excludedUserIds, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT u.*
            FROM users u
            WHERE u.account_enabled = true
              AND u.email_verified = true
              AND (
                lower(u.username) LIKE concat(lower(:q), '%')
                OR lower(u.first_name) LIKE concat(lower(:q), '%')
                OR lower(u.last_name) LIKE concat(lower(:q), '%')
                OR to_tsvector('simple', coalesce(u.username, '') || ' ' || coalesce(u.first_name, '') || ' ' || coalesce(u.last_name, '')) @@ plainto_tsquery('simple', :q)
              )
            """);

        if (viewerId != null) {
            sql.append(" AND u.id != :viewerId ");
        }

        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            sql.append(" AND u.id NOT IN (:excludedIds) ");
        }

        sql.append("""
            ORDER BY
              CASE
                WHEN lower(u.username) = lower(:q) THEN 0
                WHEN lower(u.username) LIKE lower(concat(:q, '%')) THEN 1
                WHEN lower(u.first_name) LIKE lower(concat(:q, '%')) THEN 2
                ELSE 3
              END,
              u.created_at DESC, u.id DESC
            LIMIT :lim
            """);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), User.class);
        nativeQuery.setParameter("q", query);
        nativeQuery.setParameter("lim", limit);
        if (viewerId != null) {
            nativeQuery.setParameter("viewerId", viewerId);
        }
        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            nativeQuery.setParameter("excludedIds", excludedUserIds);
        }

        return nativeQuery.getResultList();
    }

    /**
     * Heuristic candidate discovery for people (mutual connections, shared communities, active users).
     */
    @SuppressWarnings("unchecked")
    public List<User> discoverPeople(Long viewerId, Set<Long> excludedUserIds, int limit) {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT u.*
            FROM users u
            WHERE u.account_enabled = true
              AND u.email_verified = true
            """);

        if (viewerId != null) {
            sql.append(" AND u.id != :viewerId ");
        }
        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            sql.append(" AND u.id NOT IN (:excludedIds) ");
        }

        sql.append("""
            ORDER BY u.created_at DESC, u.id DESC
            LIMIT :lim
            """);

        Query nativeQuery = entityManager.createNativeQuery(sql.toString(), User.class);
        nativeQuery.setParameter("lim", limit);
        if (viewerId != null) {
            nativeQuery.setParameter("viewerId", viewerId);
        }
        if (excludedUserIds != null && !excludedUserIds.isEmpty()) {
            nativeQuery.setParameter("excludedIds", excludedUserIds);
        }

        return nativeQuery.getResultList();
    }

    /**
     * Batch checks which of the target user IDs the viewer is following.
     */
    public Set<Long> findFollowingUserIds(Long viewerId, Collection<Long> targetIds) {
        if (viewerId == null || targetIds == null || targetIds.isEmpty()) {
            return Collections.emptySet();
        }
        String jpql = "SELECT f.following.id FROM UserFollow f WHERE f.follower.id = :viewerId AND f.following.id IN :targetIds";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("viewerId", viewerId);
        query.setParameter("targetIds", targetIds);
        return new HashSet<>(query.getResultList());
    }

    public record ConnectionStatusSummary(boolean connected, boolean pending) {}

    /**
     * Batch checks connection states between viewer and target users.
     */
    public Map<Long, ConnectionStatusSummary> findConnectionStatuses(Long viewerId, Collection<Long> targetIds) {
        if (viewerId == null || targetIds == null || targetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String jpql = """
            SELECT c FROM UserConnection c
            WHERE (c.requester.id = :viewerId AND c.addressee.id IN :targetIds)
               OR (c.requester.id IN :targetIds AND c.addressee.id = :viewerId)
            """;
        TypedQuery<UserConnection> query = entityManager.createQuery(jpql, UserConnection.class);
        query.setParameter("viewerId", viewerId);
        query.setParameter("targetIds", targetIds);

        List<UserConnection> list = query.getResultList();
        Map<Long, ConnectionStatusSummary> map = new HashMap<>();
        for (UserConnection c : list) {
            Long otherUserId = c.getRequester().getId().equals(viewerId) ? c.getAddressee().getId() : c.getRequester().getId();
            boolean isAccepted = c.getStatus() == ConnectionStatus.ACCEPTED;
            boolean isPending = c.getStatus() == ConnectionStatus.PENDING;
            map.put(otherUserId, new ConnectionStatusSummary(isAccepted, isPending));
        }
        return map;
    }
}
