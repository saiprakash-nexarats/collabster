package com.collabsphere_first.explore.search;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.cursor.ExploreSearchCursor;
import com.collabsphere_first.explore.dto.ExploreCommunitySearchResponse;
import com.collabsphere_first.explore.dto.ExploreCommunitySummary;
import com.collabsphere_first.explore.repository.ExploreCommunityRepository;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunitySearchService {

    private final ExploreProperties exploreProperties;
    private final ExploreCommunityRepository communityRepository;
    private final ExploreCursorCodec cursorCodec;
    private final ExploreQueryValidator queryValidator;

    @Transactional(readOnly = true)
    public List<ExploreCommunitySummary> searchPreview(String rawQuery, User viewer, int limit) {
        ExploreCommunitySearchResponse page = searchCommunities(rawQuery, viewer, null, limit);
        return page.items();
    }

    @Transactional(readOnly = true)
    public ExploreCommunitySearchResponse searchCommunities(String rawQuery, User viewer, String cursorString, int limit) {
        String normalizedQuery = queryValidator.validateAndNormalize(rawQuery);
        int safeLimit = sanitizeLimit(limit);
        ExploreSearchCursor cursor = cursorCodec.decodeSearchCursor(cursorString, normalizedQuery);
        int currentPosition = cursor != null ? cursor.position() : 0;

        // Authoritative membership awareness: find active communities viewer has joined
        Set<UUID> allJoinedIds = communityRepository.findAllActiveCommunityIdsForUser(viewer.getId());

        int fetchLimit = Math.max(safeLimit * 3, 50);
        List<Community> rawCommunities = communityRepository.searchCommunities(
                normalizedQuery,
                allJoinedIds,
                fetchLimit
        );

        int totalFound = rawCommunities.size();
        int from = Math.min(currentPosition, totalFound);
        int to = Math.min(from + safeLimit, totalFound);
        List<Community> pageSlice = rawCommunities.subList(from, to);

        if (pageSlice.isEmpty()) {
            return new ExploreCommunitySearchResponse(Collections.emptyList(), null, false);
        }

        // Batch membership enrichment
        List<UUID> targetIds = pageSlice.stream().map(Community::getId).toList();
        Set<UUID> joinedIds = communityRepository.findJoinedCommunityIds(viewer.getId(), targetIds);

        List<ExploreCommunitySummary> summaries = new ArrayList<>(pageSlice.size());
        for (Community c : pageSlice) {
            boolean joined = joinedIds.contains(c.getId());
            boolean canJoin = c.getVisibility() != Visibility.INVITE_ONLY || joined;

            summaries.add(new ExploreCommunitySummary(
                    c.getId(),
                    c.getName(),
                    c.getSlug(),
                    c.getDescription(),
                    c.getCoverUrl(),
                    c.getMemberCount(),
                    c.getCategory() != null ? c.getCategory().name() : null,
                    c.getTags() != null ? new ArrayList<>(c.getTags()) : Collections.emptyList(),
                    joined,
                    canJoin
            ));
        }

        boolean hasMore = to < totalFound;
        String nextCursor = null;
        if (hasMore) {
            ExploreSearchCursor next = new ExploreSearchCursor(
                    ExploreCursorCodec.computeQueryHash(normalizedQuery),
                    0.0,
                    null,
                    null,
                    null,
                    to
            );
            nextCursor = cursorCodec.encodeSearchCursor(next);
        }

        return new ExploreCommunitySearchResponse(summaries, nextCursor, hasMore);
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
