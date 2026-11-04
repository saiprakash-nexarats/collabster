package com.collabsphere_first.explore.repository;

import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ExplorePostSearchRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public record RawCandidate(UUID id, double rank, Instant createdAt, Long authorId) {}

    /**
     * Searches active, non-private platform posts matching text query.
     * Uses idx_platform_posts_search_tsv GIN index directly via to_tsvector('english', coalesce(p.body, '')).
     */
    @SuppressWarnings("unchecked")
    public List<RawCandidate> searchPlatformPosts(String query, int limit) {
        String sql = """
            SELECT p.id AS id,
                   ts_rank(to_tsvector('english', coalesce(p.body, '')), plainto_tsquery('english', :q)) AS rank,
                   p.created_at AS created_at,
                   p.author_id AS author_id
            FROM platform_posts p
            WHERE p.status = :activeStatus
              AND p.visibility != :privateVisibility
              AND to_tsvector('english', coalesce(p.body, '')) @@ plainto_tsquery('english', :q)
            ORDER BY rank DESC, p.created_at DESC, p.id DESC
            LIMIT :lim
            """;

        Query nativeQuery = entityManager.createNativeQuery(sql, Tuple.class);
        nativeQuery.setParameter("q", query);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("activeStatus", PostStatus.ACTIVE.name());
        nativeQuery.setParameter("privateVisibility", PostVisibility.PRIVATE.name());

        List<Tuple> tuples = nativeQuery.getResultList();
        List<RawCandidate> results = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            UUID id = t.get("id", UUID.class);
            Number rankNum = t.get("rank", Number.class);
            Instant createdAt = t.get("created_at", Instant.class);
            Long authorId = t.get("author_id", Long.class);
            results.add(new RawCandidate(id, rankNum != null ? rankNum.doubleValue() : 0.0, createdAt, authorId));
        }
        return results;
    }

    /**
     * Searches visible community posts in active communities matching text query.
     * Uses idx_posts_search_tsv GIN index directly and enforces authoritative ModerationStatus.VISIBLE.
     */
    @SuppressWarnings("unchecked")
    public List<RawCandidate> searchCommunityPosts(String query, int limit) {
        String sql = """
            SELECT p.id AS id,
                   ts_rank(to_tsvector('english', coalesce(p.title, '') || ' ' || coalesce(p.body, '')), plainto_tsquery('english', :q)) AS rank,
                   p.created_at AS created_at,
                   p.author_id AS author_id
            FROM posts p
            JOIN communities c ON p.community_id = c.id
            WHERE p.moderation_status = :visibleStatus
              AND c.community_status = :communityActiveStatus
              AND c.archived_at IS NULL
              AND to_tsvector('english', coalesce(p.title, '') || ' ' || coalesce(p.body, '')) @@ plainto_tsquery('english', :q)
            ORDER BY rank DESC, p.created_at DESC, p.id DESC
            LIMIT :lim
            """;

        Query nativeQuery = entityManager.createNativeQuery(sql, Tuple.class);
        nativeQuery.setParameter("q", query);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("visibleStatus", ModerationStatus.VISIBLE.name());
        nativeQuery.setParameter("communityActiveStatus", CommunityStatus.ACTIVE.name());

        List<Tuple> tuples = nativeQuery.getResultList();
        List<RawCandidate> results = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            UUID id = t.get("id", UUID.class);
            Number rankNum = t.get("rank", Number.class);
            Instant createdAt = t.get("created_at", Instant.class);
            Long authorId = t.get("author_id", Long.class);
            results.add(new RawCandidate(id, rankNum != null ? rankNum.doubleValue() : 0.0, createdAt, authorId));
        }
        return results;
    }

    /**
     * Fetches recent active, public platform posts.
     */
    @SuppressWarnings("unchecked")
    public List<RawCandidate> findRecentPlatformPosts(int limit) {
        String sql = """
            SELECT p.id AS id,
                   1.0 AS rank,
                   p.created_at AS created_at,
                   p.author_id AS author_id
            FROM platform_posts p
            WHERE p.status = :activeStatus
              AND p.visibility = :publicVisibility
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :lim
            """;

        Query nativeQuery = entityManager.createNativeQuery(sql, Tuple.class);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("activeStatus", PostStatus.ACTIVE.name());
        nativeQuery.setParameter("publicVisibility", PostVisibility.PUBLIC.name());

        List<Tuple> tuples = nativeQuery.getResultList();
        List<RawCandidate> results = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            UUID id = t.get("id", UUID.class);
            Instant createdAt = t.get("created_at", Instant.class);
            Long authorId = t.get("author_id", Long.class);
            results.add(new RawCandidate(id, 1.0, createdAt, authorId));
        }
        return results;
    }

    /**
     * Fetches recent visible posts from active public communities.
     */
    @SuppressWarnings("unchecked")
    public List<RawCandidate> findRecentCommunityPosts(int limit) {
        String sql = """
            SELECT p.id AS id,
                   1.0 AS rank,
                   p.created_at AS created_at,
                   p.author_id AS author_id
            FROM posts p
            JOIN communities c ON p.community_id = c.id
            WHERE p.moderation_status = :visibleStatus
              AND c.community_status = :communityActiveStatus
              AND c.archived_at IS NULL
              AND c.visibility = :communityPublicVisibility
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :lim
            """;

        Query nativeQuery = entityManager.createNativeQuery(sql, Tuple.class);
        nativeQuery.setParameter("lim", limit);
        nativeQuery.setParameter("visibleStatus", ModerationStatus.VISIBLE.name());
        nativeQuery.setParameter("communityActiveStatus", CommunityStatus.ACTIVE.name());
        nativeQuery.setParameter("communityPublicVisibility", Visibility.PUBLIC.name());

        List<Tuple> tuples = nativeQuery.getResultList();
        List<RawCandidate> results = new ArrayList<>(tuples.size());
        for (Tuple t : tuples) {
            UUID id = t.get("id", UUID.class);
            Instant createdAt = t.get("created_at", Instant.class);
            Long authorId = t.get("author_id", Long.class);
            results.add(new RawCandidate(id, 1.0, createdAt, authorId));
        }
        return results;
    }
}

