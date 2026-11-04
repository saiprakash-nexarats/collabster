package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.discovery.ExploreRecentService;
import com.collabsphere_first.explore.dto.ExploreRecentPostsResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExplorePostSearchRepository;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExploreRecentServiceTest {

    @Mock
    private ExplorePostSearchRepository postSearchRepository;

    @Mock
    private ExploreContentHydrator contentHydrator;

    private ExploreProperties exploreProperties;
    private ExploreCursorCodec cursorCodec;
    private ExploreRecentService recentService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        cursorCodec = new ExploreCursorCodec(new ObjectMapper());

        recentService = new ExploreRecentService(
                exploreProperties,
                postSearchRepository,
                contentHydrator,
                cursorCodec
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("Recent discovery orders items by createdAt DESC and supports mixed content types")
    void testRecentPostsOrdering() {
        Instant now = Instant.now();
        Instant fiveMinAgo = now.minus(5, ChronoUnit.MINUTES);
        Instant tenMinAgo = now.minus(10, ChronoUnit.MINUTES);

        UUID pId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();

        // Platform post created 5 minutes ago
        ExplorePostSearchRepository.RawCandidate pCandidate =
                new ExplorePostSearchRepository.RawCandidate(pId, 1.0, fiveMinAgo, 1L);
        // Community post created 10 minutes ago
        ExplorePostSearchRepository.RawCandidate cCandidate =
                new ExplorePostSearchRepository.RawCandidate(cId, 1.0, tenMinAgo, 2L);

        when(postSearchRepository.findRecentPlatformPosts(anyInt()))
                .thenReturn(List.of(pCandidate));
        when(postSearchRepository.findRecentCommunityPosts(anyInt()))
                .thenReturn(List.of(cCandidate));

        FeedItemResponse pItem = new FeedItemResponse(
                ContentRef.platformPost(pId), ContentType.PLATFORM_POST, List.of(), null, null, "Recent Platform", List.of(), null, null, null, fiveMinAgo, fiveMinAgo
        );
        FeedItemResponse cItem = new FeedItemResponse(
                ContentRef.communityPost(cId), ContentType.COMMUNITY_POST, List.of(), null, "Recent Community", "Body", List.of(), null, null, null, tenMinAgo, tenMinAgo
        );

        when(contentHydrator.hydrateAndEnrich(anyList(), eq(viewer)))
                .thenReturn(List.of(pItem, cItem));

        ExploreRecentPostsResponse response = recentService.getRecentPage(viewer, null, 10);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).contentRef()).isEqualTo(ContentRef.platformPost(pId));
        assertThat(response.items().get(1).contentRef()).isEqualTo(ContentRef.communityPost(cId));
    }
}
