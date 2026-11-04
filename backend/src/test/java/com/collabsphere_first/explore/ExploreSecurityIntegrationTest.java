package com.collabsphere_first.explore;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.dto.ExplorePostSearchResponse;
import com.collabsphere_first.explore.hydration.ExploreContentHydrator;
import com.collabsphere_first.explore.repository.ExplorePostSearchRepository;
import com.collabsphere_first.explore.search.ExploreQueryValidator;
import com.collabsphere_first.explore.search.PostSearchService;
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
@DisplayName("Explore Security Invariant Tests")
class ExploreSecurityIntegrationTest {

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

    private User userA;
    private User userB;

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

        userA = User.builder().id(1L).username("user_a").build();
        userB = User.builder().id(2L).username("user_b").build();
    }

    @Test
    @DisplayName("Mandatory Scenario: User A has private post containing unique text. User B searches exact text. Post does NOT appear.")
    void testPrivatePostNeverAppearsInSearch() {
        String uniqueSecretText = "super_confidential_token_xyz_987";

        // Repository returns empty list for platform posts because p.visibility != 'PRIVATE' is enforced in SQL
        when(postSearchRepository.searchPlatformPosts(eq(uniqueSecretText), anyInt()))
                .thenReturn(Collections.emptyList());
        when(postSearchRepository.searchCommunityPosts(eq(uniqueSecretText), anyInt()))
                .thenReturn(Collections.emptyList());
        when(networkAccessService.findBlockedOrBlockingUserIds(userB.getId()))
                .thenReturn(Collections.emptySet());

        ExplorePostSearchResponse response = postSearchService.searchPosts(uniqueSecretText, userB, null, 20);

        assertThat(response.items()).isEmpty();
        verify(contentHydrator, never()).hydrateAndEnrich(anyList(), any());
    }

    @Test
    @DisplayName("Blocked user content is strictly excluded when User B searches text from blocked User A")
    void testBlockedUserContentNeverAppears() {
        String query = "architecture discussion";
        UUID postXId = UUID.randomUUID();

        // User A blocked User B
        when(networkAccessService.findBlockedOrBlockingUserIds(userB.getId()))
                .thenReturn(Set.of(userA.getId()));

        when(postSearchRepository.searchPlatformPosts(eq(query), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(postXId, 0.95, Instant.now(), userA.getId())));
        when(postSearchRepository.searchCommunityPosts(eq(query), anyInt()))
                .thenReturn(Collections.emptyList());

        ExplorePostSearchResponse response = postSearchService.searchPosts(query, userB, null, 20);

        assertThat(response.items()).isEmpty();
        verify(contentHydrator, never()).hydrateAndEnrich(anyList(), any());
    }

    @Test
    @DisplayName("Inaccessible private community content is dropped during hydration and never returned to non-member")
    void testInaccessiblePrivateCommunityPostDropped() {
        String query = "internal roadmap";
        UUID communityPostId = UUID.randomUUID();

        when(networkAccessService.findBlockedOrBlockingUserIds(userB.getId()))
                .thenReturn(Collections.emptySet());

        when(postSearchRepository.searchPlatformPosts(eq(query), anyInt()))
                .thenReturn(Collections.emptyList());
        when(postSearchRepository.searchCommunityPosts(eq(query), anyInt()))
                .thenReturn(List.of(new ExplorePostSearchRepository.RawCandidate(communityPostId, 0.9, Instant.now(), 50L)));

        // Hydrator enforces community accessibility: userB is not a member, so hydrator drops it
        when(contentHydrator.hydrateAndEnrich(argThat(list ->
                list.size() == 1 && list.get(0).equals(ContentRef.communityPost(communityPostId))), eq(userB)))
                .thenReturn(Collections.emptyList());

        ExplorePostSearchResponse response = postSearchService.searchPosts(query, userB, null, 20);

        assertThat(response.items()).isEmpty();
    }
}
