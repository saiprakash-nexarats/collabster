package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.dto.ExplorePostSearchResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExplorePostSearchRepository;
import com.collabsphere_first.explore.search.ExploreQueryValidator;
import com.collabsphere_first.explore.search.PostSearchService;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostSearchServiceTest {

    @Mock
    private ExplorePostSearchRepository postSearchRepository;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private ExploreContentHydrator contentHydrator;

    private ExploreProperties exploreProperties;
    private ExploreCursorCodec cursorCodec;
    private ExploreQueryValidator queryValidator;
    private PostSearchService postSearchService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        cursorCodec = new ExploreCursorCodec(new ObjectMapper());
        queryValidator = new ExploreQueryValidator(exploreProperties);

        postSearchService = new PostSearchService(
                exploreProperties,
                postSearchRepository,
                networkAccessService,
                contentHydrator,
                cursorCodec,
                queryValidator
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("Searches both PlatformPost and CommunityPost and merges candidates by rank")
    void testSearchBothPlatformAndCommunityPosts() {
        UUID platformId = UUID.randomUUID();
        UUID communityPostId = UUID.randomUUID();
        Instant now = Instant.now();

        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());

        when(postSearchRepository.searchPlatformPosts(eq("rust"), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(platformId, 0.9, now, 1L)));
        when(postSearchRepository.searchCommunityPosts(eq("rust"), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(communityPostId, 0.7, now, 2L)));

        FeedItemResponse pResponse = new FeedItemResponse(
                ContentRef.platformPost(platformId),
                ContentType.PLATFORM_POST,
                List.of(), null, null, "Platform post about rust", List.of(), null, null, null, now, now
        );
        FeedItemResponse cResponse = new FeedItemResponse(
                ContentRef.communityPost(communityPostId),
                ContentType.COMMUNITY_POST,
                List.of(), null, "Rust Community Post", "Body", List.of(), null, null, null, now, now
        );

        when(contentHydrator.hydrateAndEnrich(anyList(), eq(viewer)))
                .thenReturn(List.of(pResponse, cResponse));

        ExplorePostSearchResponse response = postSearchService.searchPosts("rust", viewer, null, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).contentType()).isEqualTo(ContentType.PLATFORM_POST);
        assertThat(response.items().get(1).contentType()).isEqualTo(ContentType.COMMUNITY_POST);
    }

    @Test
    @DisplayName("Blocked user posts are filtered out before hydration")
    void testBlockedAuthorsExcluded() {
        UUID blockedPostId = UUID.randomUUID();
        UUID cleanPostId = UUID.randomUUID();
        Instant now = Instant.now();

        Long blockedUserId = 666L;
        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Set.of(blockedUserId));

        when(postSearchRepository.searchPlatformPosts(eq("security"), anyInt()))
                .thenReturn(List.of(
                        new ExplorePostSearchRepository.RawCandidate(blockedPostId, 0.95, now, blockedUserId),
                        new ExplorePostSearchRepository.RawCandidate(cleanPostId, 0.85, now, 200L)
                ));
        when(postSearchRepository.searchCommunityPosts(eq("security"), anyInt()))
                .thenReturn(Collections.emptyList());

        FeedItemResponse cleanResponse = new FeedItemResponse(
                ContentRef.platformPost(cleanPostId),
                ContentType.PLATFORM_POST,
                List.of(), null, null, "Clean post", List.of(), null, null, null, now, now
        );

        when(contentHydrator.hydrateAndEnrich(argThat(list ->
                list.size() == 1 && list.get(0).id().equals(cleanPostId)), eq(viewer)))
                .thenReturn(List.of(cleanResponse));

        ExplorePostSearchResponse response = postSearchService.searchPosts("security", viewer, null, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contentRef().id()).isEqualTo(cleanPostId);
    }

    @Test
    @DisplayName("Same UUID cross-type remains independent in post search")
    void testSameUuidCrossTypeIndependence() {
        UUID sharedId = UUID.randomUUID();
        Instant now = Instant.now();

        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());

        when(postSearchRepository.searchPlatformPosts(eq("testing"), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(sharedId, 0.8, now, 10L)));
        when(postSearchRepository.searchCommunityPosts(eq("testing"), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(sharedId, 0.7, now, 20L)));

        FeedItemResponse platformItem = new FeedItemResponse(
                ContentRef.platformPost(sharedId),
                ContentType.PLATFORM_POST,
                List.of(), null, null, "Platform post", List.of(), null, null, null, now, now
        );
        FeedItemResponse communityItem = new FeedItemResponse(
                ContentRef.communityPost(sharedId),
                ContentType.COMMUNITY_POST,
                List.of(), null, "Community Title", "Community post", List.of(), null, null, null, now, now
        );

        when(contentHydrator.hydrateAndEnrich(anyList(), eq(viewer)))
                .thenReturn(List.of(platformItem, communityItem));

        ExplorePostSearchResponse response = postSearchService.searchPosts("testing", viewer, null, 20);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).contentRef()).isEqualTo(ContentRef.platformPost(sharedId));
        assertThat(response.items().get(1).contentRef()).isEqualTo(ContentRef.communityPost(sharedId));
    }
}
