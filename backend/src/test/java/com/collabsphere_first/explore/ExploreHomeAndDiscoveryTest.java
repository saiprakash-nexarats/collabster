package com.collabsphere_first.explore;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.discovery.ExploreCommunityDiscoveryService;
import com.collabsphere_first.explore.discovery.ExplorePeopleDiscoveryService;
import com.collabsphere_first.explore.discovery.ExploreRecentService;
import com.collabsphere_first.explore.dto.*;
import com.collabsphere_first.explore.repository.ExploreUserRepository;
import com.collabsphere_first.explore.search.CommunitySearchService;
import com.collabsphere_first.explore.search.GlobalExploreSearchService;
import com.collabsphere_first.explore.search.PeopleSearchService;
import com.collabsphere_first.explore.search.PostSearchService;
import com.collabsphere_first.explore.service.impl.ExploreServiceImpl;
import com.collabsphere_first.explore.trending.ExploreTrendingService;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Explore Home and Discovery Unit Tests")
class ExploreHomeAndDiscoveryTest {

    @Mock
    private ExploreTrendingService trendingService;

    @Mock
    private ExploreRecentService recentService;

    @Mock
    private ExplorePeopleDiscoveryService peopleDiscoveryService;

    @Mock
    private ExploreCommunityDiscoveryService communityDiscoveryService;

    @Mock
    private GlobalExploreSearchService globalSearchService;

    @Mock
    private PostSearchService postSearchService;

    @Mock
    private PeopleSearchService peopleSearchService;

    @Mock
    private CommunitySearchService communitySearchService;

    private ExploreProperties exploreProperties;
    private ExploreServiceImpl exploreService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        exploreService = new ExploreServiceImpl(
                exploreProperties,
                trendingService,
                recentService,
                peopleDiscoveryService,
                communityDiscoveryService,
                globalSearchService,
                postSearchService,
                peopleSearchService,
                communitySearchService
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("Explore home returns valid bounded preview sections even if some or all sections are empty")
    void testExploreHomeEmptySectionsValid() {
        when(trendingService.getTrendingPreview(eq(viewer), anyInt())).thenReturn(Collections.emptyList());
        when(recentService.getRecentPreview(eq(viewer), anyInt())).thenReturn(Collections.emptyList());
        when(peopleDiscoveryService.getPeoplePreview(eq(viewer), anyInt())).thenReturn(Collections.emptyList());
        when(communityDiscoveryService.getCommunityPreview(eq(viewer), anyInt())).thenReturn(Collections.emptyList());

        ExploreHomeResponse homeResponse = exploreService.getExploreHome(viewer);

        assertThat(homeResponse).isNotNull();
        assertThat(homeResponse.trendingPosts().items()).isEmpty();
        assertThat(homeResponse.trendingPosts().hasMore()).isFalse();
        assertThat(homeResponse.recentPosts().items()).isEmpty();
        assertThat(homeResponse.people().items()).isEmpty();
        assertThat(homeResponse.communities().items()).isEmpty();
    }

    @Test
    @DisplayName("People discovery filters already-followed and already-connected users")
    void testPeopleDiscoveryExclusions() {
        ExploreUserRepository userRepo = mock(ExploreUserRepository.class);
        NetworkAccessService netService = mock(NetworkAccessService.class);
        ExploreCursorCodec codec = new ExploreCursorCodec(new ObjectMapper());

        ExplorePeopleDiscoveryService service = new ExplorePeopleDiscoveryService(
                exploreProperties,
                userRepo,
                netService,
                codec
        );

        when(netService.findBlockedOrBlockingUserIds(100L)).thenReturn(Set.of(500L));
        when(netService.findMutedUserIds(100L)).thenReturn(Set.of(600L));
        when(netService.findFollowingUserIds(eq(100L), anyInt())).thenReturn(List.of(700L));
        when(netService.findConnectedUserIds(eq(100L), anyInt())).thenReturn(List.of(800L));

        User candidate = User.builder().id(900L).username("new_person").firstName("New").lastName("Person").build();
        when(userRepo.discoverPeople(eq(100L), argThat(set ->
                set.containsAll(Set.of(500L, 600L, 700L, 800L))), anyInt()))
                .thenReturn(List.of(candidate));

        when(userRepo.findFollowingUserIds(eq(100L), anyCollection())).thenReturn(Collections.emptySet());
        when(userRepo.findConnectionStatuses(eq(100L), anyCollection())).thenReturn(Collections.emptyMap());

        ExplorePeopleDiscoveryResponse response = service.getPeopleDiscovery(viewer, null, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).userId()).isEqualTo(900L);
        assertThat(response.items().get(0).following()).isFalse();
        assertThat(response.items().get(0).connected()).isFalse();
    }
}
