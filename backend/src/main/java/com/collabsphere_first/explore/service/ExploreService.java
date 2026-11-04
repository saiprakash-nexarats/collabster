package com.collabsphere_first.explore.service;

import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.user.entity.User;

public interface ExploreService {

    /**
     * Mobile Explore landing page returning bounded preview sections.
     */
    ExploreHomeResponse getExploreHome(User viewer);

    /**
     * Grouped global search preview across posts, people, and communities.
     */
    ExploreGroupedSearchResponse searchGrouped(String query, User viewer);

    /**
     * Typed, paginated post search across platform and community posts.
     */
    ExplorePostSearchResponse searchPosts(String query, User viewer, String cursor, int limit);

    /**
     * Typed, paginated people search.
     */
    ExplorePeopleSearchResponse searchPeople(String query, User viewer, String cursor, int limit);

    /**
     * Typed, paginated community search.
     */
    ExploreCommunitySearchResponse searchCommunities(String query, User viewer, String cursor, int limit);

    /**
     * Full trending posts discovery endpoint.
     */
    ExploreTrendingPostsResponse getTrendingPosts(User viewer, String cursor, int limit);

    /**
     * Full recent posts discovery endpoint.
     */
    ExploreRecentPostsResponse getRecentPosts(User viewer, String cursor, int limit);

    /**
     * Full people discovery endpoint.
     */
    ExplorePeopleDiscoveryResponse getPeopleDiscovery(User viewer, String cursor, int limit);

    /**
     * Full community discovery endpoint.
     */
    ExploreCommunityDiscoveryResponse getCommunityDiscovery(User viewer, String cursor, int limit);
}
