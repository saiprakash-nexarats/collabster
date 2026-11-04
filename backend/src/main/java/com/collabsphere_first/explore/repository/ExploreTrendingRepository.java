package com.collabsphere_first.explore.repository;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
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
public class ExploreTrendingRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    public record TrendingRawStats(
            ContentRef contentRef,
            long likes,
            long saves,
            long shares,
            long views,
            long watchTimeMs
    ) {}

    public static class MutableStats {
        public long likes = 0;
        public long saves = 0;
        public long shares = 0;
        public long views = 0;
        public long watchTimeMs = 0;
    }

    /**
     * Aggregates interaction signals across content_likes, content_saves, and content_interaction_events
     * within the given time window.
     */
    @SuppressWarnings("unchecked")
    public List<TrendingRawStats> aggregateTrendingInteractions(Instant windowStart, int maxCandidates) {
        Map<ContentRef, MutableStats> map = new HashMap<>();

        // 1. Likes within window
        String likesSql = """
            SELECT l.content_type AS ctype, l.content_id AS cid, count(l.id) AS cnt
            FROM content_likes l
            WHERE l.created_at >= :winStart
            GROUP BY l.content_type, l.content_id
            """;
        Query likesQuery = entityManager.createNativeQuery(likesSql, Tuple.class);
        likesQuery.setParameter("winStart", windowStart);
        List<Tuple> likesTuples = likesQuery.getResultList();
        for (Tuple t : likesTuples) {
            String typeStr = t.get("ctype", String.class);
            UUID id = t.get("cid", UUID.class);
            Number cnt = t.get("cnt", Number.class);
            if (typeStr != null && id != null) {
                try {
                    ContentType ct = ContentType.valueOf(typeStr);
                    ContentRef ref = new ContentRef(ct, id);
                    map.computeIfAbsent(ref, r -> new MutableStats()).likes += (cnt != null ? cnt.longValue() : 0);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 2. Saves within window
        String savesSql = """
            SELECT s.content_type AS ctype, s.content_id AS cid, count(s.id) AS cnt
            FROM content_saves s
            WHERE s.created_at >= :winStart
            GROUP BY s.content_type, s.content_id
            """;
        Query savesQuery = entityManager.createNativeQuery(savesSql, Tuple.class);
        savesQuery.setParameter("winStart", windowStart);
        List<Tuple> savesTuples = savesQuery.getResultList();
        for (Tuple t : savesTuples) {
            String typeStr = t.get("ctype", String.class);
            UUID id = t.get("cid", UUID.class);
            Number cnt = t.get("cnt", Number.class);
            if (typeStr != null && id != null) {
                try {
                    ContentType ct = ContentType.valueOf(typeStr);
                    ContentRef ref = new ContentRef(ct, id);
                    map.computeIfAbsent(ref, r -> new MutableStats()).saves += (cnt != null ? cnt.longValue() : 0);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 3. Events (SHARE, VIEW, WATCH_TIME) within window
        String eventsSql = """
            SELECT e.content_type AS ctype, e.content_id AS cid, e.event_type AS etype,
                   count(e.id) AS cnt, coalesce(sum(e.duration_ms), 0) AS dur
            FROM content_interaction_events e
            WHERE e.created_at >= :winStart
              AND e.event_type IN ('SHARE', 'VIEW', 'WATCH_TIME')
            GROUP BY e.content_type, e.content_id, e.event_type
            """;
        Query eventsQuery = entityManager.createNativeQuery(eventsSql, Tuple.class);
        eventsQuery.setParameter("winStart", windowStart);
        List<Tuple> eventsTuples = eventsQuery.getResultList();
        for (Tuple t : eventsTuples) {
            String typeStr = t.get("ctype", String.class);
            UUID id = t.get("cid", UUID.class);
            String eventTypeStr = t.get("etype", String.class);
            Number cnt = t.get("cnt", Number.class);
            Number dur = t.get("dur", Number.class);

            if (typeStr != null && id != null && eventTypeStr != null) {
                try {
                    ContentType ct = ContentType.valueOf(typeStr);
                    ContentRef ref = new ContentRef(ct, id);
                    MutableStats stats = map.computeIfAbsent(ref, r -> new MutableStats());
                    long count = cnt != null ? cnt.longValue() : 0;
                    long duration = dur != null ? dur.longValue() : 0;

                    switch (eventTypeStr) {
                        case "SHARE" -> stats.shares += count;
                        case "VIEW" -> stats.views += count;
                        case "WATCH_TIME" -> stats.watchTimeMs += duration;
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        List<TrendingRawStats> results = new ArrayList<>(map.size());
        for (Map.Entry<ContentRef, MutableStats> entry : map.entrySet()) {
            MutableStats s = entry.getValue();
            results.add(new TrendingRawStats(entry.getKey(), s.likes, s.saves, s.shares, s.views, s.watchTimeMs));
        }

        return results;
    }
}
