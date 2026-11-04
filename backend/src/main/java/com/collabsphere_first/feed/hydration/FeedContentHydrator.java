package com.collabsphere_first.feed.hydration;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class FeedContentHydrator {

    private final Map<ContentType, FeedContentAdapter> adapters = new EnumMap<>(ContentType.class);

    public FeedContentHydrator(List<FeedContentAdapter> adapterList) {
        if (adapterList != null) {
            for (FeedContentAdapter adapter : adapterList) {
                adapters.put(adapter.supportedType(), adapter);
            }
        }
    }

    public Map<ContentRef, HydratedFeedItem> hydrate(Collection<ContentRef> refs, User viewer) {
        if (refs == null || refs.isEmpty() || viewer == null) {
            return Collections.emptyMap();
        }

        Map<ContentType, Set<UUID>> groupedIds = new EnumMap<>(ContentType.class);
        for (ContentRef ref : refs) {
            if (ref != null && ref.type() != null && ref.id() != null) {
                groupedIds.computeIfAbsent(ref.type(), k -> new HashSet<>()).add(ref.id());
            }
        }

        Map<ContentRef, HydratedFeedItem> result = new HashMap<>();
        for (Map.Entry<ContentType, Set<UUID>> entry : groupedIds.entrySet()) {
            FeedContentAdapter adapter = adapters.get(entry.getKey());
            if (adapter != null) {
                Map<UUID, HydratedFeedItem> hydratedSlice = adapter.batchHydrate(entry.getValue(), viewer);
                for (Map.Entry<UUID, HydratedFeedItem> item : hydratedSlice.entrySet()) {
                    result.put(new ContentRef(entry.getKey(), item.getKey()), item.getValue());
                }
            } else {
                log.warn("No FeedContentAdapter registered for content type {}", entry.getKey());
            }
        }
        return result;
    }
}
