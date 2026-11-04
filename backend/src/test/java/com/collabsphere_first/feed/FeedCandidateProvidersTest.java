package com.collabsphere_first.feed;

import com.collabsphere_first.community.dto.CommunityFeedCandidate;
import com.collabsphere_first.community.service.CommunityFeedReadService;
import com.collabsphere_first.feed.candidate.*;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.service.PlatformPostFeedReadService;
import com.collabsphere_first.user.entity.User;
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
class FeedCandidateProvidersTest {

    @Mock
    private PlatformPostFeedReadService platformPostFeedReadService;

    @Mock
    private CommunityFeedReadService communityFeedReadService;

    private FeedProperties feedProperties;
    private User viewer;

    @BeforeEach
    void setUp() {
        feedProperties = new FeedProperties();
        viewer = User.builder().id(100L).username("viewer").build();
    }

    @Test
    @DisplayName("SelfPostCandidateProvider collects recent own public and followers-only posts")
    void testSelfPostCandidateProvider() {
        SelfPostCandidateProvider provider = new SelfPostCandidateProvider(platformPostFeedReadService);
        assertThat(provider.sourceType()).isEqualTo(FeedSourceType.SELF);

        UUID postId1 = UUID.randomUUID();
        PlatformPost post1 = PlatformPost.builder()
                .id(postId1)
                .author(viewer)
                .visibility(PostVisibility.PUBLIC)
                .createdAt(Instant.now())
                .build();

        when(platformPostFeedReadService.findCandidatePostsByAuthors(
                eq(List.of(100L)),
                eq(List.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS_ONLY)),
                anyInt()
        )).thenReturn(List.of(post1));

        FeedCandidateContext context = new FeedCandidateContext(
                viewer,
                feedProperties,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        List<FeedCandidate> candidates = provider.collect(context);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).contentRef().id()).isEqualTo(postId1);
        assertThat(candidates.get(0).authorId()).isEqualTo(100L);
        assertThat(candidates.get(0).sources()).containsExactly(FeedSourceType.SELF);
    }

    @Test
    @DisplayName("FollowingPostCandidateProvider collects posts from followed users, excluding blocked/muted")
    void testFollowingPostCandidateProvider() {
        FollowingPostCandidateProvider provider = new FollowingPostCandidateProvider(platformPostFeedReadService);
        assertThat(provider.sourceType()).isEqualTo(FeedSourceType.FOLLOWING);

        User followedUser = User.builder().id(200L).username("followed").build();
        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(followedUser)
                .visibility(PostVisibility.PUBLIC)
                .createdAt(Instant.now())
                .build();

        when(platformPostFeedReadService.findCandidatePostsByAuthors(
                eq(Set.of(200L)),
                eq(List.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS_ONLY)),
                anyInt()
        )).thenReturn(List.of(post));

        FeedCandidateContext context = new FeedCandidateContext(
                viewer,
                feedProperties,
                Set.of(200L, 300L), // 300 is blocked
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(300L) // blocked
        );

        List<FeedCandidate> candidates = provider.collect(context);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).authorId()).isEqualTo(200L);
        assertThat(candidates.get(0).sources()).containsExactly(FeedSourceType.FOLLOWING);
    }

    @Test
    @DisplayName("ConnectionPostCandidateProvider collects posts from connected users")
    void testConnectionPostCandidateProvider() {
        ConnectionPostCandidateProvider provider = new ConnectionPostCandidateProvider(platformPostFeedReadService);
        assertThat(provider.sourceType()).isEqualTo(FeedSourceType.CONNECTION);

        User connectedUser = User.builder().id(400L).username("connected").build();
        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(connectedUser)
                .visibility(PostVisibility.PUBLIC)
                .createdAt(Instant.now())
                .build();

        when(platformPostFeedReadService.findCandidatePostsByAuthors(
                eq(Set.of(400L)),
                eq(List.of(PostVisibility.PUBLIC, PostVisibility.FOLLOWERS_ONLY)),
                anyInt()
        )).thenReturn(List.of(post));

        FeedCandidateContext context = new FeedCandidateContext(
                viewer,
                feedProperties,
                Collections.emptySet(),
                Set.of(400L),
                Collections.emptySet(),
                Collections.emptySet()
        );

        List<FeedCandidate> candidates = provider.collect(context);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).authorId()).isEqualTo(400L);
        assertThat(candidates.get(0).sources()).containsExactly(FeedSourceType.CONNECTION);
    }

    @Test
    @DisplayName("JoinedCommunityPostCandidateProvider collects community candidates and suppresses blocked/muted authors")
    void testJoinedCommunityPostCandidateProvider() {
        JoinedCommunityPostCandidateProvider provider = new JoinedCommunityPostCandidateProvider(communityFeedReadService);
        assertThat(provider.sourceType()).isEqualTo(FeedSourceType.JOINED_COMMUNITY);

        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();
        UUID commId = UUID.randomUUID();

        CommunityFeedCandidate cand1 = new CommunityFeedCandidate(post1, 500L, Instant.now(), commId, "Java Devs");
        CommunityFeedCandidate cand2 = new CommunityFeedCandidate(post2, 600L, Instant.now(), commId, "Java Devs"); // muted

        when(communityFeedReadService.findRecentEligiblePosts(eq(viewer), anyInt()))
                .thenReturn(List.of(cand1, cand2));

        FeedCandidateContext context = new FeedCandidateContext(
                viewer,
                feedProperties,
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(600L), // muted
                Collections.emptySet()
        );

        List<FeedCandidate> candidates = provider.collect(context);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).contentRef().id()).isEqualTo(post1);
        assertThat(candidates.get(0).sources()).containsExactly(FeedSourceType.JOINED_COMMUNITY);
    }

    @Test
    @DisplayName("PublicFallbackCandidateProvider collects public posts excluding viewer, blocked, and muted authors")
    void testPublicFallbackCandidateProvider() {
        PublicFallbackCandidateProvider provider = new PublicFallbackCandidateProvider(platformPostFeedReadService);
        assertThat(provider.sourceType()).isEqualTo(FeedSourceType.PUBLIC_FALLBACK);

        User stranger = User.builder().id(700L).username("stranger").build();
        UUID postId = UUID.randomUUID();
        PlatformPost post = PlatformPost.builder()
                .id(postId)
                .author(stranger)
                .visibility(PostVisibility.PUBLIC)
                .createdAt(Instant.now())
                .build();

        when(platformPostFeedReadService.findCandidatePublicPosts(
                argThat(set -> set.contains(100L) && set.contains(800L) && set.contains(900L)),
                anyInt()
        )).thenReturn(List.of(post));

        FeedCandidateContext context = new FeedCandidateContext(
                viewer,
                feedProperties,
                Collections.emptySet(),
                Collections.emptySet(),
                Set.of(800L), // muted
                Set.of(900L)  // blocked
        );

        List<FeedCandidate> candidates = provider.collect(context);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).authorId()).isEqualTo(700L);
        assertThat(candidates.get(0).sources()).containsExactly(FeedSourceType.PUBLIC_FALLBACK);
    }
}
