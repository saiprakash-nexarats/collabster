package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.controller.FeedController;
import com.collabsphere_first.feed.dto.*;
import com.collabsphere_first.feed.exception.*;
import com.collabsphere_first.feed.service.FeedService;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedController REST API Tests")
class FeedControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FeedService feedService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private FeedController feedController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feedController)
                .setControllerAdvice(new FeedExceptionHandler())
                .build();

        currentUser = User.builder().id(100L).username("testuser").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("GET /api/v1/feed returns 200 OK with items and cursor")
    void testGetFeedSuccess() throws Exception {
        UUID postId = UUID.randomUUID();
        FeedItemResponse item = new FeedItemResponse(
                ContentRef.platformPost(postId),
                ContentType.PLATFORM_POST,
                List.of(FeedSourceType.FOLLOWING),
                AuthorSummaryResponse.builder().id(200L).username("author1").build(),
                null,
                "Hello world",
                Collections.emptyList(),
                new InteractionStateDto(true, false),
                new EngagementDto(10L, 2),
                null,
                Instant.now(),
                Instant.now()
        );

        FeedPageResponse page = new FeedPageResponse(List.of(item), "cursor123", true);

        when(feedService.getFeed(eq(currentUser), isNull(), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/feed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].contentRef.id").value(postId.toString()))
                .andExpect(jsonPath("$.data.items[0].contentType").value("PLATFORM_POST"))
                .andExpect(jsonPath("$.data.items[0].source[0]").value("FOLLOWING"))
                .andExpect(jsonPath("$.data.items[0].interactionState.liked").value(true))
                .andExpect(jsonPath("$.data.items[0].engagement.likeCount").value(10))
                .andExpect(jsonPath("$.data.nextCursor").value("cursor123"))
                .andExpect(jsonPath("$.data.hasMore").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/feed with cursor and limit passes parameters to service")
    void testGetFeedWithCursorAndLimit() throws Exception {
        FeedPageResponse page = new FeedPageResponse(Collections.emptyList(), null, false);

        when(feedService.getFeed(eq(currentUser), eq("cursor-token"), eq(15)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/feed")
                        .param("cursor", "cursor-token")
                        .param("limit", "15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/feed returns 400 Bad Request on invalid cursor format")
    void testInvalidCursorReturns400() throws Exception {
        when(feedService.getFeed(eq(currentUser), eq("bad-cursor"), anyInt()))
                .thenThrow(new InvalidFeedCursorException("Malformed cursor format"));

        mockMvc.perform(get("/api/v1/feed")
                        .param("cursor", "bad-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed cursor format"));
    }

    @Test
    @DisplayName("GET /api/v1/feed returns 410 Gone with FEED_SESSION_EXPIRED code on expired session")
    void testExpiredSessionReturns410() throws Exception {
        when(feedService.getFeed(eq(currentUser), eq("expired-cursor"), anyInt()))
                .thenThrow(new FeedSessionExpiredException("Session expired"));

        mockMvc.perform(get("/api/v1/feed")
                        .param("cursor", "expired-cursor"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("FEED_SESSION_EXPIRED"));
    }

    @Test
    @DisplayName("GET /api/v1/feed returns 403 Forbidden when accessing another user's session")
    void testAccessDeniedReturns403() throws Exception {
        when(feedService.getFeed(eq(currentUser), eq("unauthorized-cursor"), anyInt()))
                .thenThrow(new FeedSessionAccessDeniedException("Access denied to feed session"));

        mockMvc.perform(get("/api/v1/feed")
                        .param("cursor", "unauthorized-cursor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied to feed session"));
    }

    @Test
    @DisplayName("GET /api/v1/feed returns 503 Service Unavailable when storage fails")
    void testStorageErrorReturns503() throws Exception {
        when(feedService.getFeed(eq(currentUser), any(), anyInt()))
                .thenThrow(new FeedSessionStorageException("Redis unreachable", new RuntimeException()));

        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Feed session service is temporarily unavailable. Please retry."));
    }
}
