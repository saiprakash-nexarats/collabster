package com.collabsphere_first.explore.search;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreSearchCursor;
import com.collabsphere_first.explore.dto.ExplorePostSearchResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExplorePostSearchRepository;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.network.service.NetworkAccessService;
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
public class PostSearchService {

    private final ExploreProperties exploreProperties;
    private final ExplorePostSearchRepository postSearchRepository;
    private final NetworkAccessService networkAccessService;
    private final ExploreContentHydrator contentHydrator;
    private final ExploreCursorCodec cursorCodec;
    private final ExploreQueryValidator queryValidator;

    public record SearchCandidate(ContentRef contentRef, double rank, Instant createdAt, Long authorId) {}

    @Transactional(readOnly = true)
    public List<FeedItemResponse> searchPreview(String rawQuery, User viewer, int limit) {
        ExplorePostSearchResponse page = searchPosts(rawQuery, viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExplorePostSearchResponse searchPosts(String rawQuery, User viewer, String cursorString, int limit) {
        String normalizedQuery = queryValidator.validateAndNormalize(rawQuery);
        int safeLimit = sanitizeLimit(limit);
        ExploreSearchCursor cursor = cursorCodec.decodeSearchCursor(cursorString, normalizedQuery);
        int currentPosition = cursor != null ? cursor.position() : 0;

        // Fetch candidate matches from both PlatformPost and CommunityPost
        int fetchCount = Math.max(safeLimit * 4, 100);
        List<ExplorePostSearchRepository.RawCandidate> platformRaw = postSearchRepository.searchPlatformPosts(normalizedQuery, fetchCount);
        List<ExplorePostSearchRepository.RawCandidate> communityRaw = postSearchRepository.searchCommunityPosts(normalizedQuery, fetchCount);

        // Pre-filter blocked relationships in both directions
        Set<Long> blockedOrBlockingUserIds = networkAccessService.findBlockedOrBlockingUserIds(viewer.getId());

        List<SearchCandidate> candidates = new ArrayList<>(platformRaw.size() + communityRaw.size());
        for (ExplorePostSearchRepository.RawCandidate r : platformRaw) {
            if (r.authorId() != null && blockedOrBlockingUserIds.contains(r.authorId())) {
                continue;
            }
            candidates.add(new SearchCandidate(new ContentRef(ContentType.PLATFORM_POST, r.id()), r.rank(), r.createdAt(), r.authorId()));
        }
        for (ExplorePostSearchRepository.RawCandidate r : communityRaw) {
            if (r.authorId() != null && blockedOrBlockingUserIds.contains(r.authorId())) {
                continue;
            }
            candidates.add(new SearchCandidate(new ContentRef(ContentType.COMMUNITY_POST, r.id()), r.rank(), r.createdAt(), r.authorId()));
        }

        // Deterministic ranking: rank DESC, createdAt DESC, id DESC, type ASC
        candidates.sort(Comparator
                .comparingDouble(SearchCandidate::rank).reversed()
                .thenComparing(SearchCandidate::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(c -> c.contentRef().id(), Comparator.reverseOrder())
                .thenComparing(c -> c.contentRef().type()));

        List<FeedItemResponse> validItems = new ArrayList<>();
        int scanPosition = currentPosition;

        while (validItems.size() < safeLimit && scanPosition < candidates.size()) {
            int batchWindow = Math.min(safeLimit * 2, candidates.size() - scanPosition);
            List<SearchCandidate> candidateSlice = candidates.subList(scanPosition, scanPosition + batchWindow);

            List<ContentRef> refsToHydrate = candidateSlice.stream()
                    .map(SearchCandidate::contentRef)
                    .toList();

            List<FeedItemResponse> hydratedList = contentHydrator.hydrateAndEnrich(refsToHydrate, viewer);
            Map<ContentRef, FeedItemResponse> itemMap = new HashMap<>();
            for (FeedItemResponse item : hydratedList) {
                itemMap.put(item.contentRef(), item);
            }

            for (SearchCandidate cand : candidateSlice) {
                scanPosition++;
                FeedItemResponse hydrated = itemMap.get(cand.contentRef());
                if (hydrated != null) {
                    validItems.add(hydrated);
                    if (validItems.size() == safeLimit) {
                        break;
                    }
                }
            }
        }

        boolean hasMore = scanPosition < candidates.size();
        String nextCursor = null;
        if (hasMore && !validItems.isEmpty()) {
            FeedItemResponse last = validItems.get(validItems.size() - 1);
            ExploreSearchCursor next = new ExploreSearchCursor(
                    ExploreCursorCodec.computeQueryHash(normalizedQuery),
                    0.0,
                    last.createdAt(),
                    last.contentRef().id(),
                    last.contentType(),
                    scanPosition
            );
            nextCursor = cursorCodec.encodeSearchCursor(next);
        }

        return new ExplorePostSearchResponse(validItems, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getSearch().getDefaultPageSize();
        int maxSize = exploreProperties.getSearch().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
