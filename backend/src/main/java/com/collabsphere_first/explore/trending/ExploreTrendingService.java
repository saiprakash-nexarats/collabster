package com.collabsphere_first.explore.trending;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreKeysetCursor;
import com.collabsphere_first.explore.dto.ExploreTrendingPostsResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExploreTrendingRepository;
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
public class ExploreTrendingService {

    private final ExploreProperties exploreProperties;
    private final ExploreTrendingRepository trendingRepository;
    private final TrendingScoreCalculator scoreCalculator;
    private final ExploreContentHydrator contentHydrator;
    private final ExploreCursorCodec cursorCodec;

    public record ScoredCandidate(ContentRef contentRef, double score) {}

    @Transactional(readOnly = true)
    public List<FeedItemResponse> getTrendingPreview(User viewer, int limit) {
        ExploreTrendingPostsResponse page = getTrendingPage(viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExploreTrendingPostsResponse getTrendingPage(User viewer, String cursorString, int limit) {
        int safeLimit = sanitizeLimit(limit);
        ExploreKeysetCursor cursor = cursorCodec.decodeKeysetCursor(cursorString);
        int currentPosition = cursor != null ? cursor.position() : 0;

        Instant now = Instant.now();
        Instant windowStart = now.minus(exploreProperties.getTrending().getWindow());
        int candidateLimit = exploreProperties.getTrending().getCandidateLimit();

        List<ExploreTrendingRepository.TrendingRawStats> rawStats =
                trendingRepository.aggregateTrendingInteractions(windowStart, candidateLimit);

        List<ScoredCandidate> scoredCandidates = new ArrayList<>(rawStats.size());
        for (ExploreTrendingRepository.TrendingRawStats stat : rawStats) {
            double score = scoreCalculator.calculateScore(
                    stat.likes(),
                    stat.saves(),
                    stat.shares(),
                    stat.views(),
                    stat.watchTimeMs(),
                    null, // Base score calculation
                    now
            );
            scoredCandidates.add(new ScoredCandidate(stat.contentRef(), score));
        }

        // Deterministic sorting: score DESC, id DESC, contentType ASC
        scoredCandidates.sort(Comparator
                .comparingDouble(ScoredCandidate::score).reversed()
                .thenComparing(c -> c.contentRef().id(), Comparator.reverseOrder())
                .thenComparing(c -> c.contentRef().type()));

        List<FeedItemResponse> validItems = new ArrayList<>();
        int scanPosition = currentPosition;

        while (validItems.size() < safeLimit && scanPosition < scoredCandidates.size()) {
            int batchWindow = Math.min(safeLimit * 2, scoredCandidates.size() - scanPosition);
            List<ScoredCandidate> candidateSlice = scoredCandidates.subList(scanPosition, scanPosition + batchWindow);

            List<ContentRef> refsToHydrate = candidateSlice.stream()
                    .map(ScoredCandidate::contentRef)
                    .toList();

            List<FeedItemResponse> hydratedList = contentHydrator.hydrateAndEnrich(refsToHydrate, viewer);
            Map<ContentRef, FeedItemResponse> itemMap = new HashMap<>();
            for (FeedItemResponse item : hydratedList) {
                itemMap.put(item.contentRef(), item);
            }

            for (ScoredCandidate cand : candidateSlice) {
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

        boolean hasMore = scanPosition < scoredCandidates.size();
        String nextCursor = null;
        if (hasMore && !validItems.isEmpty()) {
            FeedItemResponse last = validItems.get(validItems.size() - 1);
            ExploreKeysetCursor next = new ExploreKeysetCursor(
                    last.createdAt(),
                    last.contentRef().id(),
                    null,
                    last.contentType(),
                    null,
                    scanPosition
            );
            nextCursor = cursorCodec.encodeKeysetCursor(next);
        }

        return new ExploreTrendingPostsResponse(validItems, nextCursor, hasMore);
    }

    private int sanitizeLimit(int limit) {
        int defaultSize = exploreProperties.getHome().getTrendingPreviewLimit();
        int maxSize = exploreProperties.getSearch().getMaxPageSize();
        if (limit <= 0) {
            return defaultSize;
        }
        return Math.min(limit, maxSize);
    }
}
