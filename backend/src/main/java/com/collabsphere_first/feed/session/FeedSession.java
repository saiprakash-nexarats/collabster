package com.collabsphere_first.feed.session;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public record FeedSession(
    @JsonProperty("sessionId") String sessionId,
    @JsonProperty("userId") Long userId,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("orderedContentRefs") List<ContentRef> orderedContentRefs,
    @JsonProperty("candidateSources") Map<String, Set<FeedSourceType>> candidateSources
) implements Serializable {

    public FeedSession {
        orderedContentRefs = orderedContentRefs != null ? Collections.unmodifiableList(orderedContentRefs) : Collections.emptyList();
        candidateSources = candidateSources != null ? Collections.unmodifiableMap(candidateSources) : Collections.emptyMap();
    }

    public static FeedSession create(
            String sessionId,
            Long userId,
            Instant createdAt,
            List<ContentRef> orderedContentRefs,
            Map<ContentRef, Set<FeedSourceType>> sourcesMap
    ) {
        Map<String, Set<FeedSourceType>> stringKeyMap = new HashMap<>();
        if (sourcesMap != null) {
            for (Map.Entry<ContentRef, Set<FeedSourceType>> entry : sourcesMap.entrySet()) {
                if (entry.getKey() != null) {
                    String key = entry.getKey().type().name() + ":" + entry.getKey().id().toString();
                    stringKeyMap.put(key, entry.getValue());
                }
            }
        }
        return new FeedSession(sessionId, userId, createdAt, orderedContentRefs, stringKeyMap);
    }

    public Set<FeedSourceType> getSources(ContentRef ref) {
        if (candidateSources == null || ref == null) {
            return Collections.emptySet();
        }
        String key = ref.type().name() + ":" + ref.id().toString();
        Set<FeedSourceType> sources = candidateSources.get(key);
        return sources != null ? sources : Collections.emptySet();
    }
}
