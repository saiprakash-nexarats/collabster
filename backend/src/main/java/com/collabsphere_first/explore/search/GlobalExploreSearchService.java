package com.collabsphere_first.explore.search;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalExploreSearchService {

    private final ExploreProperties exploreProperties;
    private final PostSearchService postSearchService;
    private final PeopleSearchService peopleSearchService;
    private final CommunitySearchService communitySearchService;
    private final ExploreQueryValidator queryValidator;

    /**
     * Executes grouped preview search across posts, people, and communities.
     *
     * @param rawQuery user input search query
     * @param viewer authenticated user
     * @return grouped preview search results
     */
    @Transactional(readOnly = true)
    public ExploreGroupedSearchResponse searchGrouped(String rawQuery, User viewer) {
        String normalizedQuery = queryValidator.validateAndNormalize(rawQuery);
        int previewLimit = exploreProperties.getSearch().getPreviewLimit();

        List<FeedItemResponse> postItems = postSearchService.searchPreview(normalizedQuery, viewer, previewLimit);
        List<ExplorePersonSummary> peopleItems = peopleSearchService.searchPreview(normalizedQuery, viewer, previewLimit);
        List<ExploreCommunitySummary> communityItems = communitySearchService.searchPreview(normalizedQuery, viewer, previewLimit);

        ExploreSection<FeedItemResponse> postSection = ExploreSection.of(
                postItems,
                postItems.size() >= previewLimit,
                "/api/v1/explore/search/posts?q=" + normalizedQuery
        );

        ExploreSection<ExplorePersonSummary> peopleSection = ExploreSection.of(
                peopleItems,
                peopleItems.size() >= previewLimit,
                "/api/v1/explore/search/people?q=" + normalizedQuery
        );

        ExploreSection<ExploreCommunitySummary> communitySection = ExploreSection.of(
                communityItems,
                communityItems.size() >= previewLimit,
                "/api/v1/explore/search/communities?q=" + normalizedQuery
        );

        return new ExploreGroupedSearchResponse(
                normalizedQuery,
                postSection,
                peopleSection,
                communitySection
        );
    }
}
