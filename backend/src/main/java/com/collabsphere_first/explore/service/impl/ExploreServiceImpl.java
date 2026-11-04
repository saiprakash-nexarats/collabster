package com.collabsphere_first.explore.service.impl;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.discovery.ExploreCommunityDiscoveryService;
import com.collabsphere_first.explore.discovery.ExplorePeopleDiscoveryService;
import com.collabsphere_first.explore.discovery.ExploreRecentService;
import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.explore.search.CommunitySearchService;
import com.collabsphere_first.explore.search.GlobalExploreSearchService;
import com.collabsphere_first.explore.search.PeopleSearchService;
import com.collabsphere_first.explore.search.PostSearchService;
import com.collabsphere_first.explore.service.ExploreService;
import com.collabsphere_first.explore.trending.ExploreTrendingService;
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
public class ExploreServiceImpl implements ExploreService {

    private final ExploreProperties exploreProperties;
    private final ExploreTrendingService trendingService;
    private final ExploreRecentService recentService;
    private final ExplorePeopleDiscoveryService peopleDiscoveryService;
    private final ExploreCommunityDiscoveryService communityDiscoveryService;
    private final GlobalExploreSearchService globalSearchService;
    private final PostSearchService postSearchService;
    private final PeopleSearchService peopleSearchService;
    private final CommunitySearchService communitySearchService;

    @Override
    @Transactional(readOnly = true)
    public ExploreHomeResponse getExploreHome(User viewer) {
        ExploreProperties.HomeProperties homeProps = exploreProperties.getHome();

        List<FeedItemResponse> trendingItems = trendingService.getTrendingPreview(viewer, homeProps.getTrendingPreviewLimit());
        List<FeedItemResponse> recentItems = recentService.getRecentPreview(viewer, homeProps.getRecentPreviewLimit());
        List<ExplorePersonSummary> peopleItems = peopleDiscoveryService.getPeoplePreview(viewer, homeProps.getPeoplePreviewLimit());
        List<ExploreCommunitySummary> communityItems = communityDiscoveryService.getCommunityPreview(viewer, homeProps.getCommunityPreviewLimit());

        ExploreSection<FeedItemResponse> trendingSection = ExploreSection.of(
                trendingItems,
                trendingItems.size() >= homeProps.getTrendingPreviewLimit(),
                "/api/v1/explore/posts/trending"
        );

        ExploreSection<FeedItemResponse> recentSection = ExploreSection.of(
                recentItems,
                recentItems.size() >= homeProps.getRecentPreviewLimit(),
                "/api/v1/explore/posts/recent"
        );

        ExploreSection<ExplorePersonSummary> peopleSection = ExploreSection.of(
                peopleItems,
                peopleItems.size() >= homeProps.getPeoplePreviewLimit(),
                "/api/v1/explore/people"
        );

        ExploreSection<ExploreCommunitySummary> communitySection = ExploreSection.of(
                communityItems,
                communityItems.size() >= homeProps.getCommunityPreviewLimit(),
                "/api/v1/explore/communities"
        );

        return new ExploreHomeResponse(
                trendingSection,
                recentSection,
                peopleSection,
                communitySection
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ExploreGroupedSearchResponse searchGrouped(String query, User viewer) {
        return globalSearchService.searchGrouped(query, viewer);
    }

    @Override
    @Transactional(readOnly = true)
    public ExplorePostSearchResponse searchPosts(String query, User viewer, String cursor, int limit) {
        return postSearchService.searchPosts(query, viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExplorePeopleSearchResponse searchPeople(String query, User viewer, String cursor, int limit) {
        return peopleSearchService.searchPeople(query, viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExploreCommunitySearchResponse searchCommunities(String query, User viewer, String cursor, int limit) {
        return communitySearchService.searchCommunities(query, viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExploreTrendingPostsResponse getTrendingPosts(User viewer, String cursor, int limit) {
        return trendingService.getTrendingPage(viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExploreRecentPostsResponse getRecentPosts(User viewer, String cursor, int limit) {
        return recentService.getRecentPage(viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExplorePeopleDiscoveryResponse getPeopleDiscovery(User viewer, String cursor, int limit) {
        return peopleDiscoveryService.getPeopleDiscovery(viewer, cursor, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public ExploreCommunityDiscoveryResponse getCommunityDiscovery(User viewer, String cursor, int limit) {
        return communityDiscoveryService.getCommunityDiscovery(viewer, cursor, limit);
    }
}
