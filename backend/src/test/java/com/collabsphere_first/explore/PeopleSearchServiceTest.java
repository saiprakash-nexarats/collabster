package com.collabsphere_first.explore;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.dto.ExplorePeopleSearchResponse;
import com.collabsphere_first.explore.dto.ExplorePersonSummary;
import com.collabsphere_first.explore.repository.ExploreUserRepository;
import com.collabsphere_first.explore.search.ExploreQueryValidator;
import com.collabsphere_first.explore.search.PeopleSearchService;
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
class PeopleSearchServiceTest {

    @Mock
    private ExploreUserRepository exploreUserRepository;

    @Mock
    private NetworkAccessService networkAccessService;

    private ExploreProperties exploreProperties;
    private ExploreCursorCodec cursorCodec;
    private ExploreQueryValidator queryValidator;
    private PeopleSearchService peopleSearchService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        cursorCodec = new ExploreCursorCodec(new ObjectMapper());
        queryValidator = new ExploreQueryValidator(exploreProperties);

        peopleSearchService = new PeopleSearchService(
                exploreProperties,
                exploreUserRepository,
                networkAccessService,
                cursorCodec,
                queryValidator
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("People search finds users and enriches with following and connection state without N+1 queries")
    void testSearchPeopleSuccess() {
        User alice = User.builder()
                .id(201L)
                .username("alice_w")
                .firstName("Alice")
                .lastName("Wonder")
                .bio("Software Architect")
                .profilePicture("https://cdn.example.com/alice.jpg")
                .build();

        User bob = User.builder()
                .id(202L)
                .username("alices_friend")
                .firstName("Bob")
                .lastName("Smith")
                .bio("Rust Dev")
                .build();

        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());
        when(exploreUserRepository.searchUsers(eq("alice"), eq(100L), anySet(), anyInt()))
                .thenReturn(List.of(alice, bob));

        when(exploreUserRepository.findFollowingUserIds(eq(100L), anyCollection()))
                .thenReturn(Set.of(201L)); // Following alice

        when(exploreUserRepository.findConnectionStatuses(eq(100L), anyCollection()))
                .thenReturn(Map.of(202L, new ExploreUserRepository.ConnectionStatusSummary(true, false))); // Connected to bob

        ExplorePeopleSearchResponse response = peopleSearchService.searchPeople("alice", viewer, null, 20);

        assertThat(response.items()).hasSize(2);

        ExplorePersonSummary s1 = response.items().get(0);
        assertThat(s1.userId()).isEqualTo(201L);
        assertThat(s1.username()).isEqualTo("alice_w");
        assertThat(s1.displayName()).isEqualTo("Alice Wonder");
        assertThat(s1.following()).isTrue();
        assertThat(s1.connected()).isFalse();

        ExplorePersonSummary s2 = response.items().get(1);
        assertThat(s2.userId()).isEqualTo(202L);
        assertThat(s2.username()).isEqualTo("alices_friend");
        assertThat(s2.following()).isFalse();
        assertThat(s2.connected()).isTrue();
    }

    @Test
    @DisplayName("Blocked users are passed into repository exclusion set")
    void testBlockedUsersPassedToRepository() {
        Set<Long> blockedIds = Set.of(501L, 502L);
        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(blockedIds);
        when(exploreUserRepository.searchUsers(eq("charlie"), eq(100L), eq(blockedIds), anyInt()))
                .thenReturn(Collections.emptyList());

        ExplorePeopleSearchResponse response = peopleSearchService.searchPeople("charlie", viewer, null, 20);

        assertThat(response.items()).isEmpty();
        verify(exploreUserRepository).searchUsers("charlie", 100L, blockedIds, 60);
    }
}
