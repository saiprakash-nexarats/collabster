package com.collabsphere_first.feed;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.feed.candidate.FeedCandidate;
import com.collabsphere_first.feed.candidate.FeedCandidateProvider;
import com.collabsphere_first.feed.candidate.FeedSourceType;
import com.collabsphere_first.feed.config.FeedProperties;
import com.collabsphere_first.feed.cursor.FeedCursorCodec;
import com.collabsphere_first.feed.dto.FeedItemResponse;
import com.collabsphere_first.feed.dto.FeedPageResponse;
import com.collabsphere_first.feed.exception.FeedSessionAccessDeniedException;
import com.collabsphere_first.feed.exception.FeedSessionExpiredException;
import com.collabsphere_first.feed.hydration.FeedContentHydrator;
import com.collabsphere_first.feed.hydration.HydratedFeedItem;
import com.collabsphere_first.feed.ranking.BaselineFeedRankingStrategy;
import com.collabsphere_first.feed.ranking.FeedRankingStrategy;
import com.collabsphere_first.feed.service.impl.FeedServiceImpl;
import com.collabsphere_first.feed.session.FeedSession;
import com.collabsphere_first.feed.session.FeedSessionStore;
import com.collabsphere_first.interaction.dto.InteractionSummary;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.post.dto.response.AuthorSummaryResponse;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock
    private FeedCandidateProvider candidateProvider;

    @Mock
    private FeedSessionStore sessionStore;

    @Mock
    private FeedContentHydrator contentHydrator;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private InteractionService interactionService;

    private FeedCursorCodec cursorCodec;
    private FeedProperties feedProperties;
    private FeedRankingStrategy rankingStrategy;
    private FeedServiceImpl feedService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        cursorCodec = new FeedCursorCodec();
        feedProperties = new FeedProperties();
        feedProperties.setDefaultPageSize(2);
        feedProperties.setMaxPageSize(10);
        rankingStrategy = new BaselineFeedRankingStrategy();

        feedService = new FeedServiceImpl(
                List.of(candidateProvider),
                rankingStrategy,
                sessionStore,
                cursorCodec,
                contentHydrator,
                networkAccessService,
                interactionService,
                feedProperties
        );

        currentUser = User.builder().id(100L).username("testuser").build();
    }

    @Test
    @DisplayName("Initial feed request generates session, ranks candidates, hydrates items, and returns cursor")
    void testInitialFeedRequest() {
        UUID post1Id = UUID.randomUUID();
        UUID post2Id = UUID.randomUUID();
        UUID post3Id = UUID.randomUUID();

        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef ref2 = ContentRef.platformPost(post2Id);
        ContentRef ref3 = ContentRef.communityPost(post3Id);

        Instant now = Instant.now();
        FeedCandidate cand1 = new FeedCandidate(ref1, 200L, now.minusSeconds(100), Set.of(FeedSourceType.FOLLOWING));
        FeedCandidate cand2 = new FeedCandidate(ref2, 300L, now.minusSeconds(200), Set.of(FeedSourceType.CONNECTION));
        FeedCandidate cand3 = new FeedCandidate(ref3, 400L, now.minusSeconds(300), Set.of(FeedSourceType.JOINED_COMMUNITY));

        when(candidateProvider.collect(any())).thenReturn(List.of(cand1, cand2, cand3));

        // Network mocks
        when(networkAccessService.findFollowingUserIds(eq(100L), anyInt())).thenReturn(List.of(200L));
        when(networkAccessService.findConnectedUserIds(eq(100L), anyInt())).thenReturn(List.of(300L));
        when(networkAccessService.findMutedUserIds(100L)).thenReturn(Collections.emptySet());
        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());

        // Interaction mocks
        when(interactionService.findSuppressedContentRefs(100L)).thenReturn(Collections.emptySet());
        when(interactionService.getInteractionSummaries(eq(100L), anyCollection())).thenReturn(Map.of(
                ref1, new InteractionSummary(true, false, 15L),
                ref2, new InteractionSummary(false, true, 42L),
                ref3, new InteractionSummary(false, false, 0L)
        ));

        // Hydration mock
        HydratedFeedItem item1 = new HydratedFeedItem(ref1, ContentType.PLATFORM_POST, AuthorSummaryResponse.builder().id(200L).username("a1").build(), null, "Post 1", Collections.emptyList(), null, 0, now, now);
        HydratedFeedItem item2 = new HydratedFeedItem(ref2, ContentType.PLATFORM_POST, AuthorSummaryResponse.builder().id(300L).username("a2").build(), null, "Post 2", Collections.emptyList(), null, 0, now, now);
        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(ref1, item1, ref2, item2));

        // Page size = 2
        FeedPageResponse response = feedService.getFeed(currentUser, null, 2);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(2);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();

        // Verify session was saved in store
        ArgumentCaptor<FeedSession> sessionCaptor = ArgumentCaptor.forClass(FeedSession.class);
        verify(sessionStore).saveSession(sessionCaptor.capture(), any(Duration.class));
        FeedSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.userId()).isEqualTo(100L);
        assertThat(savedSession.orderedContentRefs()).contains(ref1, ref2, ref3);

        // Verify interaction enrichment
        FeedItemResponse itemForRef1 = response.items().stream()
                .filter(i -> i.contentRef().equals(ref1))
                .findFirst().orElseThrow();
        assertThat(itemForRef1.interactionState().liked()).isTrue();
        assertThat(itemForRef1.engagement().likeCount()).isEqualTo(15L);

        FeedItemResponse itemForRef2 = response.items().stream()
                .filter(i -> i.contentRef().equals(ref2))
                .findFirst().orElseThrow();
        assertThat(itemForRef2.interactionState().saved()).isTrue();
        assertThat(itemForRef2.engagement().likeCount()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Continuing feed request with cursor loads existing session and advances position")
    void testCursorContinuation() {
        String sessionId = UUID.randomUUID().toString();
        UUID post1Id = UUID.randomUUID();
        UUID post2Id = UUID.randomUUID();
        UUID post3Id = UUID.randomUUID();

        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef ref2 = ContentRef.platformPost(post2Id);
        ContentRef ref3 = ContentRef.communityPost(post3Id);

        FeedSession session = FeedSession.create(
                sessionId,
                100L,
                Instant.now(),
                List.of(ref1, ref2, ref3),
                Map.of(ref3, Set.of(FeedSourceType.JOINED_COMMUNITY))
        );

        when(sessionStore.getSession(sessionId)).thenReturn(Optional.of(session));

        // Cursor at position 2 (reading 3rd item)
        String cursor = cursorCodec.encode(sessionId, 2);

        HydratedFeedItem item3 = new HydratedFeedItem(ref3, ContentType.COMMUNITY_POST, AuthorSummaryResponse.builder().id(400L).username("a4").build(), "Comm Title", "Body", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(ref3, item3));

        when(interactionService.getInteractionSummaries(eq(100L), anyCollection()))
                .thenReturn(Map.of(ref3, new InteractionSummary(false, false, 5L)));

        FeedPageResponse response = feedService.getFeed(currentUser, cursor, 2);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).contentRef()).isEqualTo(ref3);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("Inaccessible or deleted items are skipped during hydration and next valid items fill the page")
    void testStaleItemSkipping() {
        String sessionId = UUID.randomUUID().toString();
        UUID post1Id = UUID.randomUUID();
        UUID deletedPostId = UUID.randomUUID();
        UUID post3Id = UUID.randomUUID();

        ContentRef ref1 = ContentRef.platformPost(post1Id);
        ContentRef refDeleted = ContentRef.platformPost(deletedPostId);
        ContentRef ref3 = ContentRef.communityPost(post3Id);

        FeedSession session = FeedSession.create(
                sessionId,
                100L,
                Instant.now(),
                List.of(ref1, refDeleted, ref3),
                Collections.emptyMap()
        );

        when(sessionStore.getSession(sessionId)).thenReturn(Optional.of(session));
        String cursor = cursorCodec.encode(sessionId, 0);

        // refDeleted returns null in hydration (deleted/moderated)
        HydratedFeedItem item1 = new HydratedFeedItem(ref1, ContentType.PLATFORM_POST, null, null, "P1", Collections.emptyList(), null, 0, Instant.now(), Instant.now());
        HydratedFeedItem item3 = new HydratedFeedItem(ref3, ContentType.COMMUNITY_POST, null, "C3", "P3", Collections.emptyList(), null, 0, Instant.now(), Instant.now());

        when(contentHydrator.hydrate(anyCollection(), eq(currentUser)))
                .thenReturn(Map.of(ref1, item1, ref3, item3));

        when(interactionService.getInteractionSummaries(eq(100L), anyCollection())).thenReturn(Collections.emptyMap());

        // Limit = 2, so despite refDeleted being skipped, we still get 2 items: [ref1, ref3]
        FeedPageResponse response = feedService.getFeed(currentUser, cursor, 2);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).contentRef()).isEqualTo(ref1);
        assertThat(response.items().get(1).contentRef()).isEqualTo(ref3);
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    @DisplayName("Cross-user session access throws FeedSessionAccessDeniedException")
    void testCrossUserSessionAccessDenied() {
        String sessionId = UUID.randomUUID().toString();
        FeedSession sessionOfAnotherUser = FeedSession.create(
                sessionId,
                999L, // belongs to user 999
                Instant.now(),
                Collections.emptyList(),
                Collections.emptyMap()
        );

        when(sessionStore.getSession(sessionId)).thenReturn(Optional.of(sessionOfAnotherUser));
        String cursor = cursorCodec.encode(sessionId, 0);

        // User 100 tries to read User 999's session
        assertThatThrownBy(() -> feedService.getFeed(currentUser, cursor, 10))
                .isInstanceOf(FeedSessionAccessDeniedException.class);
    }

    @Test
    @DisplayName("Expired or missing session throws FeedSessionExpiredException")
    void testExpiredSessionThrows() {
        String sessionId = UUID.randomUUID().toString();
        when(sessionStore.getSession(sessionId)).thenReturn(Optional.empty());

        String cursor = cursorCodec.encode(sessionId, 0);

        assertThatThrownBy(() -> feedService.getFeed(currentUser, cursor, 10))
                .isInstanceOf(FeedSessionExpiredException.class);
    }

    @Test
    @DisplayName("Cold start user with zero content returns empty successful feed response")
    void testEmptyColdStartFeed() {
        when(candidateProvider.collect(any())).thenReturn(Collections.emptyList());
        when(networkAccessService.findFollowingUserIds(eq(100L), anyInt())).thenReturn(Collections.emptyList());
        when(networkAccessService.findConnectedUserIds(eq(100L), anyInt())).thenReturn(Collections.emptyList());
        when(networkAccessService.findMutedUserIds(100L)).thenReturn(Collections.emptySet());
        when(networkAccessService.findBlockedOrBlockingUserIds(100L)).thenReturn(Collections.emptySet());
        when(interactionService.findSuppressedContentRefs(100L)).thenReturn(Collections.emptySet());

        FeedPageResponse response = feedService.getFeed(currentUser, null, 10);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }
}
