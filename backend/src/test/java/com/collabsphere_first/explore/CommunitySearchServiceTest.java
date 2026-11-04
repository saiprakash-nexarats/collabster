package com.collabsphere_first.explore;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.CommunityCategory;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.cursor.ExploreCursorCodec;
import com.collabsphere_first.explore.dto.ExploreCommunitySearchResponse;
import com.collabsphere_first.explore.dto.ExploreCommunitySummary;
import com.collabsphere_first.explore.repository.ExploreCommunityRepository;
import com.collabsphere_first.explore.search.CommunitySearchService;
import com.collabsphere_first.explore.search.ExploreQueryValidator;
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
class CommunitySearchServiceTest {

    @Mock
    private ExploreCommunityRepository communityRepository;

    private ExploreProperties exploreProperties;
    private ExploreCursorCodec cursorCodec;
    private ExploreQueryValidator queryValidator;
    private CommunitySearchService communitySearchService;
    private User viewer;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        cursorCodec = new ExploreCursorCodec(new ObjectMapper());
        queryValidator = new ExploreQueryValidator(exploreProperties);

        communitySearchService = new CommunitySearchService(
                exploreProperties,
                communityRepository,
                cursorCodec,
                queryValidator
        );

        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("Community search matches communities and enriches viewer membership status")
    void testSearchCommunitiesSuccess() {
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();

        Community c1 = Community.builder()
                .id(c1Id)
                .name("Rust Developers")
                .slug("rust-devs")
                .description("Systems programming community")
                .category(CommunityCategory.TECHNOLOGY)
                .visibility(Visibility.PUBLIC)
                .memberCount(42)
                .tags(List.of("rust", "systems"))
                .build();

        Community c2 = Community.builder()
                .id(c2Id)
                .name("Rust Learning Group")
                .slug("rust-learners")
                .description("Beginner rustaceans")
                .category(CommunityCategory.EDUCATION)
                .visibility(Visibility.PUBLIC)
                .memberCount(15)
                .tags(List.of("rust", "beginners"))
                .build();

        when(communityRepository.findAllActiveCommunityIdsForUser(100L))
                .thenReturn(Set.of(c1Id)); // Viewer is a member of c1

        when(communityRepository.searchCommunities(eq("rust"), eq(Set.of(c1Id)), anyInt()))
                .thenReturn(List.of(c1, c2));

        when(communityRepository.findJoinedCommunityIds(eq(100L), anyCollection()))
                .thenReturn(Set.of(c1Id));

        ExploreCommunitySearchResponse response = communitySearchService.searchCommunities("rust", viewer, null, 20);

        assertThat(response.items()).hasSize(2);

        ExploreCommunitySummary s1 = response.items().get(0);
        assertThat(s1.id()).isEqualTo(c1Id);
        assertThat(s1.name()).isEqualTo("Rust Developers");
        assertThat(s1.joined()).isTrue();

        ExploreCommunitySummary s2 = response.items().get(1);
        assertThat(s2.id()).isEqualTo(c2Id);
        assertThat(s2.name()).isEqualTo("Rust Learning Group");
        assertThat(s2.joined()).isFalse();
    }
}
