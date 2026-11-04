package com.collabsphere_first.explore;

import com.collabsphere_first.explore.controller.ExploreController;
import com.collabsphere_first.explore.controller.ExploreExceptionHandler;
import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.explore.exception.InvalidExploreCursorException;
import com.collabsphere_first.explore.exception.InvalidExploreQueryException;
import com.collabsphere_first.explore.service.ExploreService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExploreController REST API Tests")
class ExploreControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExploreService exploreService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ExploreController exploreController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exploreController)
                .setControllerAdvice(new ExploreExceptionHandler())
                .build();

        currentUser = User.builder().id(100L).username("testuser").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("GET /api/v1/explore returns HTTP 200 with bounded landing sections")
    void testGetExploreHome() throws Exception {
        ExploreHomeResponse homeResponse = new ExploreHomeResponse(
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/posts/trending"),
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/posts/recent"),
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/people"),
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/communities")
        );

        when(exploreService.getExploreHome(currentUser)).thenReturn(homeResponse);

        mockMvc.perform(get("/api/v1/explore")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trendingPosts.seeAllUrl").value("/api/v1/explore/posts/trending"))
                .andExpect(jsonPath("$.data.recentPosts.seeAllUrl").value("/api/v1/explore/posts/recent"))
                .andExpect(jsonPath("$.data.people.seeAllUrl").value("/api/v1/explore/people"))
                .andExpect(jsonPath("$.data.communities.seeAllUrl").value("/api/v1/explore/communities"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/search returns HTTP 200 with grouped preview")
    void testSearchGrouped() throws Exception {
        ExploreGroupedSearchResponse searchResponse = new ExploreGroupedSearchResponse(
                "rust",
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/search/posts?q=rust"),
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/search/people?q=rust"),
                ExploreSection.of(Collections.emptyList(), false, "/api/v1/explore/search/communities?q=rust")
        );

        when(exploreService.searchGrouped("rust", currentUser)).thenReturn(searchResponse);

        mockMvc.perform(get("/api/v1/explore/search")
                        .param("q", "rust")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.query").value("rust"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/search with blank query returns HTTP 400")
    void testSearchBlankQueryReturnsBadRequest() throws Exception {
        when(exploreService.searchGrouped(eq(""), any()))
                .thenThrow(new InvalidExploreQueryException("Search query cannot be empty"));

        mockMvc.perform(get("/api/v1/explore/search")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Search query cannot be empty"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/search/posts with invalid cursor returns HTTP 400")
    void testSearchPostsInvalidCursorReturnsBadRequest() throws Exception {
        when(exploreService.searchPosts(eq("rust"), any(), eq("bad_cursor"), anyInt()))
                .thenThrow(new InvalidExploreCursorException("Cursor was not generated for query: rust"));

        mockMvc.perform(get("/api/v1/explore/search/posts")
                        .param("q", "rust")
                        .param("cursor", "bad_cursor")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cursor was not generated for query: rust"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/search/people returns HTTP 200 with paginated results")
    void testSearchPeopleSuccess() throws Exception {
        ExplorePeopleSearchResponse response = new ExplorePeopleSearchResponse(
                List.of(new ExplorePersonSummary(200L, "alice", "Alice Wonder", null, "Bio", false, false, false)),
                null,
                false
        );

        when(exploreService.searchPeople(eq("alice"), any(), isNull(), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/search/people")
                        .param("q", "alice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].username").value("alice"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/search/communities returns HTTP 200")
    void testSearchCommunitiesSuccess() throws Exception {
        UUID cId = UUID.randomUUID();
        ExploreCommunitySearchResponse response = new ExploreCommunitySearchResponse(
                List.of(new ExploreCommunitySummary(cId, "Rustaceans", "rustaceans", "Desc", null, 100, "TECH", List.of("rust"), false, true)),
                null,
                false
        );

        when(exploreService.searchCommunities(eq("rust"), any(), isNull(), eq(20)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/search/communities")
                        .param("q", "rust")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].name").value("Rustaceans"));
    }

    @Test
    @DisplayName("GET /api/v1/explore/posts/trending returns HTTP 200")
    void testGetTrendingPosts() throws Exception {
        ExploreTrendingPostsResponse response = new ExploreTrendingPostsResponse(
                Collections.emptyList(), null, false
        );

        when(exploreService.getTrendingPosts(currentUser, null, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/posts/trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/explore/posts/recent returns HTTP 200")
    void testGetRecentPosts() throws Exception {
        ExploreRecentPostsResponse response = new ExploreRecentPostsResponse(
                Collections.emptyList(), null, false
        );

        when(exploreService.getRecentPosts(currentUser, null, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/posts/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/explore/people returns HTTP 200")
    void testGetPeopleDiscovery() throws Exception {
        ExplorePeopleDiscoveryResponse response = new ExplorePeopleDiscoveryResponse(
                Collections.emptyList(), null, false
        );

        when(exploreService.getPeopleDiscovery(currentUser, null, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/people")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/explore/communities returns HTTP 200")
    void testGetCommunityDiscovery() throws Exception {
        ExploreCommunityDiscoveryResponse response = new ExploreCommunityDiscoveryResponse(
                Collections.emptyList(), null, false
        );

        when(exploreService.getCommunityDiscovery(currentUser, null, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/explore/communities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
