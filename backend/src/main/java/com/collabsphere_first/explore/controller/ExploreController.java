package com.collabsphere_first.explore.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.explore.service.ExploreService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/api/v1/explore", "/v1/explore"})
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;
    private final CurrentUserService currentUserService;

    /**
     * Mobile & Web Explore landing page returning bounded preview sections or search results.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getExploreHome(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", required = false, defaultValue = "all") String type
    ) {
        User viewer = null;
        try {
            viewer = currentUserService.getCurrentUser();
        } catch (Exception ignored) {}

        String term = (query != null && !query.isBlank()) ? query : q;
        if (term != null && !term.isBlank()) {
            ExploreGroupedSearchResponse searchResponse = exploreService.searchGrouped(term, viewer);
            return ResponseEntity.ok(ApiResponse.success(searchResponse));
        }

        ExploreHomeResponse response = exploreService.getExploreHome(viewer);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Grouped global search preview across posts, people, and communities.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ExploreGroupedSearchResponse>> searchGrouped(
            @RequestParam("q") String q
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExploreGroupedSearchResponse response = exploreService.searchGrouped(q, viewer);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Typed, paginated post search across platform and community posts.
     */
    @GetMapping("/search/posts")
    public ResponseEntity<ApiResponse<ExplorePostSearchResponse>> searchPosts(
            @RequestParam("q") String q,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExplorePostSearchResponse response = exploreService.searchPosts(q, viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Typed, paginated people search.
     */
    @GetMapping("/search/people")
    public ResponseEntity<ApiResponse<ExplorePeopleSearchResponse>> searchPeople(
            @RequestParam("q") String q,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExplorePeopleSearchResponse response = exploreService.searchPeople(q, viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Typed, paginated community search.
     */
    @GetMapping("/search/communities")
    public ResponseEntity<ApiResponse<ExploreCommunitySearchResponse>> searchCommunities(
            @RequestParam("q") String q,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExploreCommunitySearchResponse response = exploreService.searchCommunities(q, viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Full trending posts discovery endpoint.
     */
    @GetMapping("/posts/trending")
    public ResponseEntity<ApiResponse<ExploreTrendingPostsResponse>> getTrendingPosts(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExploreTrendingPostsResponse response = exploreService.getTrendingPosts(viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Full recent posts discovery endpoint.
     */
    @GetMapping("/posts/recent")
    public ResponseEntity<ApiResponse<ExploreRecentPostsResponse>> getRecentPosts(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExploreRecentPostsResponse response = exploreService.getRecentPosts(viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Full people discovery endpoint.
     */
    @GetMapping("/people")
    public ResponseEntity<ApiResponse<ExplorePeopleDiscoveryResponse>> getPeopleDiscovery(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExplorePeopleDiscoveryResponse response = exploreService.getPeopleDiscovery(viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Full community discovery endpoint.
     */
    @GetMapping("/communities")
    public ResponseEntity<ApiResponse<ExploreCommunityDiscoveryResponse>> getCommunityDiscovery(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        User viewer = currentUserService.getCurrentUser();
        ExploreCommunityDiscoveryResponse response = exploreService.getCommunityDiscovery(viewer, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
