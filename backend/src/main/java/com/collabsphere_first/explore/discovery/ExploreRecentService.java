package com.collabsphere_first.explore.discovery;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreKeysetCursor;
import com.collabsphere_first.explore.dto.ExploreRecentPostsResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExplorePostSearchRepository;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExploreRecentService {

    private final ExploreProperties exploreProperties;
    private final ExplorePostSearchRepository postSearchRepository;
    private final ExploreContentHydrator contentHydrator;
    private final ExploreCursorCodec cursorCodec;

    public record RecentCandidate(ContentRef contentRef, Instant createdAt) {}

    @Transactional(readOnly = true)
    public List<FeedItemResponse> getRecentPreview(User viewer, int limit) {
        ExploreRecentPostsResponse page = getRecentPage(viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExploreRecentPostsResponse getRecentPage(User viewer, String cursorString, int limit) {
        int safeLimit = sanitizeLimit(limit);
        ExploreKeysetCursor cursor = cursorCodec.decodeKeysetCursor(cursorString);

        // Fetch candidate slices from both platform posts and community posts
        int fetchCount = Math.max(safeLimit * 3, 50);
        List<ExplorePostSearchRepository.RawCandidate> platformRaw = postSearchRepository.findRecentPlatformPosts(fetchCount);
        List<ExplorePostSearchRepository.RawCandidate> communityRaw = postSearchRepository.findRecentCommunityPosts(fetchCount);

        List<RecentCandidate> candidates = new ArrayList<>(platformRaw.size() + communityRaw.size());
        for (ExplorePostSearchRepository.RawCandidate r : platformRaw) {
            candidates.add(new RecentCandidate(new ContentRef(ContentType.PLATFORM_POST, r.id()), r.createdAt()));
        }
        for (ExplorePostSearchRepository.RawCandidate r : communityRaw) {
            candidates.add(new RecentCandidate(new ContentRef(ContentType.COMMUNITY_POST, r.id()), r.createdAt()));
        }

        // Deterministic sort: createdAt DESC, id DESC, type ASC
        candidates.sort(Comparator
                .comparing(RecentCandidate::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(c -> c.contentRef().id(), Comparator.reverseOrder())
                .thenComparing(c -> c.contentRef().type()));

        // Apply keyset cursor filtering
        List<RecentCandidate> filteredByCursor = candidates;
        if (cursor != null && cursor.createdAt() != null && cursor.id() != null) {
            filteredByCursor = candidates.stream()
                    .filter(c -> {
                        if (c.createdAt() == null) return false;
                        int timeCmp = c.createdAt().compareTo(cursor.createdAt());
                        if (timeCmp < 0) return true;
                        if (timeCmp > 0) return false;
                        return c.contentRef().id().compareTo(cursor.id()) < 0;
                    })
                    .toList();
        }

        List<FeedItemResponse> validItems = new ArrayList<>();
        int index = 0;

        while (validItems.size() < safeLimit && index < filteredByCursor.size()) {
            int batchWindow = Math.min(safeLimit * 2, filteredByCursor.size() - index);
            List<RecentCandidate> slice = filteredByCursor.subList(index, index + batchWindow);

            List<ContentRef> refsToHydrate = slice.stream().map(RecentCandidate::contentRef).toList();
            List<FeedItemResponse> hydratedList = contentHydrator.hydrateAndEnrich(refsToHydrate, viewer);

            Map<ContentRef, FeedItemResponse> itemMap = new HashMap<>();
            for (FeedItemResponse item : hydratedList) {
                itemMap.put(item.contentRef(), item);
            }

            for (RecentCandidate cand : slice) {
                index++;
                FeedItemResponse hydrated = itemMap.get(cand.contentRef());
                if (hydrated != null) {
                    validItems.add(hydrated);
                    if (validItems.size() == safeLimit) {
                        break;
                    }
                }
            }
        }

        boolean hasMore = index < filteredByCursor.size();
        String nextCursor = null;
        if (hasMore && !validItems.isEmpty()) {
            FeedItemResponse last = validItems.get(validItems.size() - 1);
            ExploreKeysetCursor next = new ExploreKeysetCursor(
                    last.createdAt(),
                    last.contentRef().id(),
                    null,
                    last.contentType(),
                    null,
                    0
            );
            nextCursor = cursorCodec.encodeKeysetCursor(next);
        }

        return new ExploreRecentPostsResponse(validItems, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getHome().getRecentPreviewLimit();
        int maxSize = exploreProperties.getSearch().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
