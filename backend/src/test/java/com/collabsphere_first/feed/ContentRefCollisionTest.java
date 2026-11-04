package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedCandidateProvider;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.cursor.FeedCursorCodec;
import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.feed.hydration.FeedContentAdapter;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.service.impl.FeedServiceImpl;
import com.collabsphere_first.feed.session.FeedSession;
import com.collabsphere_first.feed.session.FeedSessionStore;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.repository.ContentInteractionEventRepository;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.repository.ContentSaveRepository;
import com.collabsphere_first.interaction.resolver.ContentResolutionService;
import com.collabsphere_first.interaction.service.InteractionServiceImpl;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
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
@DisplayName("ContentRef Collision & Integrity Tests (Phase 3.1)")
class ContentRefCollisionTest {

    @Mock
    private ContentLikeRepository contentLikeRepository;

    @Mock
    private ContentSaveRepository contentSaveRepository;

    @Mock
    private ContentInteractionEventRepository contentInteractionEventRepository;

    @Mock
    private ContentResolutionService contentResolutionService;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private FeedSessionStore sessionStore;

    @Mock
    private FeedContentHydrator contentHydrator;

    @Mock
    private FeedCandidateProvider candidateProvider;

    private InteractionServiceImpl interactionService;
    private FeedProperties feedProperties;
    private User viewer;
    private UUID sharedId;
    private ContentRef platformRef;
    private ContentRef communityRef;

    @BeforeEach
    void setUp() {
        com.collabsphere_first.interaction.adapter.PlatformPostInteractionAdapter platformAdapter =
                new com.collabsphere_first.interaction.adapter.PlatformPostInteractionAdapter(
                        contentLikeRepository, contentSaveRepository, contentResolutionService
                );
        com.collabsphere_first.interaction.adapter.CommunityPostInteractionAdapter communityAdapter =
                new com.collabsphere_first.interaction.adapter.CommunityPostInteractionAdapter(
                        contentLikeRepository, contentSaveRepository, contentResolutionService
                );
        com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry adapterRegistry =
                new com.collabsphere_first.interaction.adapter.ContentInteractionAdapterRegistry(
                        List.of(platformAdapter, communityAdapter)
                );
        interactionService = new InteractionServiceImpl(
                contentLikeRepository,
                contentSaveRepository,
                contentInteractionEventRepository,
                contentResolutionService,
                adapterRegistry
        );
        feedProperties = new FeedProperties();
        viewer = User.builder().id(100L).username("testViewer").build();

        // The critical collision case: identical UUID across PLATFORM_POST and COMMUNITY_POST
        sharedId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        platformRef = ContentRef.platformPost(sharedId);
        communityRef = ContentRef.communityPost(sharedId);
    }

    @Test
    @DisplayName("Section 4: Like counts remain strictly isolated when UUID matches across ContentTypes")
    void testLikeCountSafetyWithSharedUuid() {
        // Platform post has 10 likes, Community post has 25 likes
        when(contentLikeRepository.countLikesByContentTypeAndIds(eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, sharedId, 10L}));
        when(contentLikeRepository.countLikesByContentTypeAndIds(eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, sharedId, 25L}));

        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(
                viewer.getId(),
                List.of(platformRef, communityRef)
        );

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(platformRef).likeCount()).isEqualTo(10L);
        assertThat(summaries.get(communityRef).likeCount()).isEqualTo(25L);
        // Explicit check: neither is merged into 35
        assertThat(summaries.get(platformRef).likeCount()).isNotEqualTo(35L);
        assertThat(summaries.get(communityRef).likeCount()).isNotEqualTo(35L);
    }

    @Test
    @DisplayName("Section 5: Liked state remains isolated when UUID matches across ContentTypes")
    void testLikedStateSafetyWithSharedUuid() {
        // Scenario A: Viewer likes ONLY PlatformPost
        when(contentLikeRepository.countLikesByContentTypeAndIds(any(), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, sharedId}));
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());

        Map<ContentRef, InteractionSummary> summariesA = interactionService.getInteractionSummaries(
                100L,
                List.of(platformRef, communityRef)
        );
        assertThat(summariesA.get(platformRef).liked()).isTrue();
        assertThat(summariesA.get(communityRef).liked()).isFalse();

        // Scenario B: Viewer likes ONLY CommunityPost
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, sharedId}));

        Map<ContentRef, InteractionSummary> summariesB = interactionService.getInteractionSummaries(
                100L,
                List.of(platformRef, communityRef)
        );
        assertThat(summariesB.get(platformRef).liked()).isFalse();
        assertThat(summariesB.get(communityRef).liked()).isTrue();
    }

    @Test
    @DisplayName("Section 6: Saved state remains isolated when UUID matches across ContentTypes")
    void testSavedStateSafetyWithSharedUuid() {
        when(contentLikeRepository.countLikesByContentTypeAndIds(any(), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());

        // Viewer saves ONLY CommunityPost
        when(contentSaveRepository.findSavedContent(eq(100L), eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());
        when(contentSaveRepository.findSavedContent(eq(100L), eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, sharedId}));

        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(
                100L,
                List.of(platformRef, communityRef)
        );

        assertThat(summaries.get(platformRef).saved()).isFalse();
        assertThat(summaries.get(communityRef).saved()).isTrue();
    }

    @Test
    @DisplayName("Section 7: HIDE and NOT_INTERESTED suppression queries respect ContentType alongside UUID")
    void testSuppressionSafetyWithSharedUuid() {
        // User hides PLATFORM_POST sharedId
        when(contentInteractionEventRepository.findSuppressedContent(100L))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, sharedId}));

        Set<ContentRef> suppressed = interactionService.findSuppressedContentRefs(100L);

        assertThat(suppressed).contains(platformRef);
        assertThat(suppressed).doesNotContain(communityRef);

        // Reverse: User marks NOT_INTERESTED on COMMUNITY_POST sharedId
        when(contentInteractionEventRepository.findSuppressedContent(100L))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, sharedId}));

        Set<ContentRef> suppressedRev = interactionService.findSuppressedContentRefs(100L);

        assertThat(suppressedRev).doesNotContain(platformRef);
        assertThat(suppressedRev).contains(communityRef);
    }

    @Test
    @DisplayName("Section 8 & 9: Candidate deduplication and session metadata maintain independent entries for identical UUID")
    void testCandidateDeduplicationAndSourceMetadata() {
        FeedCandidate candPlatform = new FeedCandidate(platformRef, 200L, Instant.now(), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate candCommunity = new FeedCandidate(communityRef, 300L, Instant.now(), Set.of(FeedSourceType.JOINED_COMMUNITY));

        FeedServiceImpl feedService = new FeedServiceImpl(
                List.of(candidateProvider),
                new BaselineFeedRankingStrategy(),
                sessionStore,
                new FeedCursorCodec(),
                contentHydrator,
                networkAccessService,
                interactionService,
                feedProperties
        );

        when(candidateProvider.collect(any())).thenReturn(List.of(candPlatform, candCommunity));
        when(networkAccessService.findFollowingUserIds(anyLong(), anyInt())).thenReturn(List.of(200L));
        when(networkAccessService.findConnectedUserIds(anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(networkAccessService.findMutedUserIds(anyLong())).thenReturn(Collections.emptySet());
        when(networkAccessService.findBlockedOrBlockingUserIds(anyLong())).thenReturn(Collections.emptySet());
        when(contentInteractionEventRepository.findSuppressedContent(anyLong())).thenReturn(Collections.emptyList());

        HydratedFeedItem itemPlatform = new HydratedFeedItem(platformRef, ContentType.PLATFORM_POST,
                AuthorSummaryResponse.builder().id(200L).username("pAuthor").build(), null, "Platform Post", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        HydratedFeedItem itemCommunity = new HydratedFeedItem(communityRef, ContentType.COMMUNITY_POST,
                AuthorSummaryResponse.builder().id(300L).username("cAuthor").build(), "Comm Post", "Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());

        when(contentHydrator.hydrate(anyCollection(), eq(viewer)))
                .thenReturn(Map.of(platformRef, itemPlatform, communityRef, itemCommunity));

        when(contentLikeRepository.countLikesByContentTypeAndIds(any(), anyCollection())).thenReturn(Collections.emptyList());

        FeedPageResponse response = feedService.getFeed(viewer, null, 10);

        assertThat(response.items()).hasSize(2);
        // Verify both items coexist and are distinct
        assertThat(response.items().stream().map(i -> i.contentRef()).toList())
                .containsExactlyInAnyOrder(platformRef, communityRef);

        // Verify session was saved with both references and independent sources
        var captor = org.mockito.ArgumentCaptor.forClass(FeedSession.class);
        verify(sessionStore).saveSession(captor.capture(), any());
        FeedSession saved = captor.getValue();
        assertThat(saved.orderedContentRefs()).contains(platformRef, communityRef);
        assertThat(saved.getSources(platformRef)).containsExactly(FeedSourceType.FOLLOWING);
        assertThat(saved.getSources(communityRef)).containsExactly(FeedSourceType.JOINED_COMMUNITY);
    }

    @Test
    @DisplayName("Section 11: Hydration delegates to correct adapters and preserves order for identical UUIDs")
    void testHydrationCollisionDelegation() {
        FeedContentAdapter platformAdapter = mock(FeedContentAdapter.class);
        when(platformAdapter.supportedType()).thenReturn(ContentType.PLATFORM_POST);

        FeedContentAdapter communityAdapter = mock(FeedContentAdapter.class);
        when(communityAdapter.supportedType()).thenReturn(ContentType.COMMUNITY_POST);

        FeedContentHydrator hydrator = new FeedContentHydrator(List.of(platformAdapter, communityAdapter));

        HydratedFeedItem pItem = new HydratedFeedItem(platformRef, ContentType.PLATFORM_POST, null, null, "P", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        HydratedFeedItem cItem = new HydratedFeedItem(communityRef, ContentType.COMMUNITY_POST, null, "C", "B", Collections.emptyList(), null, 0, Instant.now(), Instant.now());

        when(platformAdapter.batchHydrate(eq(Set.of(sharedId)), eq(viewer))).thenReturn(Map.of(sharedId, pItem));
        when(communityAdapter.batchHydrate(eq(Set.of(sharedId)), eq(viewer))).thenReturn(Map.of(sharedId, cItem));

        Map<ContentRef, HydratedFeedItem> hydratedMap = hydrator.hydrate(List.of(platformRef, communityRef), viewer);

        assertThat(hydratedMap).hasSize(2);
        assertThat(hydratedMap.get(platformRef)).isSameAs(pItem);
        assertThat(hydratedMap.get(communityRef)).isSameAs(cItem);
        assertThat(hydratedMap.get(platformRef).contentType()).isEqualTo(ContentType.PLATFORM_POST);
        assertThat(hydratedMap.get(communityRef).contentType()).isEqualTo(ContentType.COMMUNITY_POST);
    }

    @Test
    @DisplayName("Section 21: Mandatory Scenario — PlatformPost X liked & hidden, CommunityPost X saved — Feed excludes PlatformPost and includes CommunityPost")
    void testMandatoryCollisionScenario() {
        UUID x = UUID.randomUUID();
        ContentRef pRef = ContentRef.platformPost(x);
        ContentRef cRef = ContentRef.communityPost(x);

        // 1. Database state:
        // User likes PlatformPost X
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, x}));
        when(contentLikeRepository.findLikedContent(eq(100L), eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());

        // User saves CommunityPost X
        when(contentSaveRepository.findSavedContent(eq(100L), eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(Collections.<Object[]>emptyList());
        when(contentSaveRepository.findSavedContent(eq(100L), eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, x}));

        // User hides PlatformPost X
        when(contentInteractionEventRepository.findSuppressedContent(100L))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, x}));

        // Community post has 7 likes, Platform post has 12 likes
        when(contentLikeRepository.countLikesByContentTypeAndIds(eq(ContentType.PLATFORM_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.PLATFORM_POST, x, 12L}));
        when(contentLikeRepository.countLikesByContentTypeAndIds(eq(ContentType.COMMUNITY_POST), anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{ContentType.COMMUNITY_POST, x, 7L}));

        // 2. Verify Interaction Summaries state:
        Map<ContentRef, InteractionSummary> summaries = interactionService.getInteractionSummaries(100L, List.of(pRef, cRef));
        InteractionSummary pSummary = summaries.get(pRef);
        InteractionSummary cSummary = summaries.get(cRef);

        assertThat(pSummary.liked()).isTrue();
        assertThat(pSummary.saved()).isFalse();
        assertThat(pSummary.likeCount()).isEqualTo(12L);

        assertThat(cSummary.liked()).isFalse();
        assertThat(cSummary.saved()).isTrue();
        assertThat(cSummary.likeCount()).isEqualTo(7L);

        // 3. Verify Suppression state:
        Set<ContentRef> suppressed = interactionService.findSuppressedContentRefs(100L);
        assertThat(suppressed).contains(pRef);
        assertThat(suppressed).doesNotContain(cRef);

        // 4. Feed Execution:
        FeedCandidate candP = new FeedCandidate(pRef, 200L, Instant.now(), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate candC = new FeedCandidate(cRef, 300L, Instant.now(), Set.of(FeedSourceType.JOINED_COMMUNITY));

        FeedServiceImpl feedService = new FeedServiceImpl(
                List.of(candidateProvider),
                new BaselineFeedRankingStrategy(),
                sessionStore,
                new FeedCursorCodec(),
                contentHydrator,
                networkAccessService,
                interactionService,
                feedProperties
        );

        when(candidateProvider.collect(any())).thenReturn(List.of(candP, candC));
        when(networkAccessService.findFollowingUserIds(anyLong(), anyInt())).thenReturn(List.of(200L));
        when(networkAccessService.findConnectedUserIds(anyLong(), anyInt())).thenReturn(Collections.emptyList());
        when(networkAccessService.findMutedUserIds(anyLong())).thenReturn(Collections.emptySet());
        when(networkAccessService.findBlockedOrBlockingUserIds(anyLong())).thenReturn(Collections.emptySet());

        HydratedFeedItem itemC = new HydratedFeedItem(cRef, ContentType.COMMUNITY_POST,
                AuthorSummaryResponse.builder().id(300L).username("commAuthor").build(), "Comm Title", "Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        when(contentHydrator.hydrate(anyCollection(), eq(viewer)))
                .thenReturn(Map.of(cRef, itemC));

        FeedPageResponse feedResponse = feedService.getFeed(viewer, null, 10);

        // PlatformPost X MUST BE EXCLUDED due to suppression
        // CommunityPost X MUST BE INCLUDED
        assertThat(feedResponse.items()).hasSize(1);
        assertThat(feedResponse.items().get(0).contentRef()).isEqualTo(cRef);
        assertThat(feedResponse.items().get(0).interactionState().liked()).isFalse();
        assertThat(feedResponse.items().get(0).interactionState().saved()).isTrue();
        assertThat(feedResponse.items().get(0).engagement().likeCount()).isEqualTo(7L);
    }
}
