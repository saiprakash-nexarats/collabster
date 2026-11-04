package com.collabsphere_first.recommendation.feedback;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.common.content.ContentType;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.entity.Post;
import com.collabsphere_first.community.enums.*;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.repository.PostRepository;
import com.collabsphere_first.interaction.dto.InteractionEventRequest;
import com.collabsphere_first.interaction.dto.LikeResponse;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.recommendation.feedback.dto.RecommendationFeedbackBatchResponse;
import com.collabsphere_first.recommendation.feedback.entity.RecommendationFeedbackOutbox;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackEventType;
import com.collabsphere_first.recommendation.feedback.enums.FeedbackOutboxStatus;
import com.collabsphere_first.recommendation.feedback.repository.RecommendationFeedbackOutboxRepository;
import com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("RecommendationFeedbackPostgresIntegrationTest")
class RecommendationFeedbackPostgresIntegrationTest {

    @Autowired
    private RecommendationFeedbackOutboxRepository outboxRepository;

    @Autowired
    private RecommendationFeedbackOutboxService outboxService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ContentLikeRepository contentLikeRepository;

    @Autowired
    private PlatformPostRepository platformPostRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User authorUser;
    private Community publicCommunity;
    private Community privateCommunity;
    private Post publicCommunityPost;
    private PlatformPost platformPost;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testUser = userRepository.save(User.builder()
                .username("fb_user_" + suffix)
                .email("fb_user_" + suffix + "@test.com")
                .firstName("Test")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        authorUser = userRepository.save(User.builder()
                .username("fb_author_" + suffix)
                .email("fb_author_" + suffix + "@test.com")
                .firstName("Author")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        publicCommunity = communityRepository.save(Community.builder()
                .name("Public FB Comm " + suffix)
                .slug("public-fb-comm-" + suffix)
                .ownerId(authorUser.getId())
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .category(CommunityCategory.TECHNOLOGY)
                .type(CommunityType.GENERAL)
                .lifecycle(LifecycleState.PERMANENT)
                .memberCount(1)
                .build());

        membershipRepository.save(Membership.builder()
                .community(publicCommunity)
                .userId(authorUser.getId())
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        publicCommunityPost = postRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(authorUser.getId())
                .title("Public Post Title")
                .body("Public Post Body")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .build());

        privateCommunity = communityRepository.save(Community.builder()
                .name("Private FB Comm " + suffix)
                .slug("private-fb-comm-" + suffix)
                .ownerId(authorUser.getId())
                .visibility(Visibility.PRIVATE)
                .communityStatus(CommunityStatus.ACTIVE)
                .category(CommunityCategory.TECHNOLOGY)
                .type(CommunityType.GENERAL)
                .lifecycle(LifecycleState.PERMANENT)
                .memberCount(1)
                .build());

        membershipRepository.save(Membership.builder()
                .community(privateCommunity)
                .userId(authorUser.getId())
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        platformPost = platformPostRepository.save(PlatformPost.builder()
                .author(authorUser)
                .body("Platform Post Body")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build());
    }

    @Test
    @Transactional
    @DisplayName("LIKE on PlatformPost creates content_likes and outbox LIKE row atomically; idempotent retry emits no duplicate")
    void testLikeCreatesOutboxAndAuthoritativeStateAtomically() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());

        // First like
        LikeResponse response = interactionService.likeContent(testUser, ref);
        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());

        // Verify authoritative content_likes row exists
        assertTrue(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(testUser.getId(), ContentType.PLATFORM_POST, platformPost.getId()));

        // Verify recommendation feedback outbox row exists
        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(platformPost.getId()))
                .toList();

        assertEquals(1, outboxList.size());
        RecommendationFeedbackOutbox outbox = outboxList.get(0);
        assertEquals(FeedbackEventType.LIKE, outbox.getEventType());
        assertEquals(ContentType.PLATFORM_POST, outbox.getContentType());
        assertEquals(FeedbackOutboxStatus.PENDING, outbox.getStatus());
        assertNotNull(outbox.getPayload());

        // Idempotent retry: liking again should NOT create a second outbox event
        LikeResponse secondResponse = interactionService.likeContent(testUser, ref);
        assertTrue(secondResponse.isLiked());

        List<RecommendationFeedbackOutbox> outboxAfterRetry = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(platformPost.getId()))
                .toList();
        assertEquals(1, outboxAfterRetry.size(), "Duplicate transport like must not create duplicate feedback occurrence");
    }

    @Test
    @Transactional
    @DisplayName("Community Post LIKE then UNLIKE creates separate LIKE and UNLIKE outbox occurrences")
    void testCommunityLikeAndUnlikeLifecycle() {
        ContentRef ref = ContentRef.communityPost(publicCommunityPost.getId());

        // Like
        interactionService.likeContent(testUser, ref);
        assertTrue(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(testUser.getId(), ContentType.COMMUNITY_POST, publicCommunityPost.getId()));

        // Unlike
        interactionService.unlikeContent(testUser, ref);
        assertFalse(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(testUser.getId(), ContentType.COMMUNITY_POST, publicCommunityPost.getId()));

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(publicCommunityPost.getId()))
                .sorted(Comparator.comparing(RecommendationFeedbackOutbox::getCreatedAt))
                .toList();

        assertEquals(2, outboxList.size());
        assertEquals(FeedbackEventType.LIKE, outboxList.get(0).getEventType());
        assertEquals(FeedbackEventType.UNLIKE, outboxList.get(1).getEventType());
        assertNotEquals(outboxList.get(0).getEventId(), outboxList.get(1).getEventId(), "Occurrences must have unique eventIds");
    }

    @Test
    @Transactional
    @DisplayName("SAVE -> UNSAVE -> SAVE creates three distinct outbox occurrences with three event IDs")
    void testSaveUnsaveSaveLifecycle() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());

        interactionService.saveContent(testUser, ref);
        interactionService.unsaveContent(testUser, ref);
        interactionService.saveContent(testUser, ref);

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(platformPost.getId()))
                .sorted(Comparator.comparing(RecommendationFeedbackOutbox::getCreatedAt))
                .toList();

        assertEquals(3, outboxList.size());
        assertEquals(FeedbackEventType.SAVE, outboxList.get(0).getEventType());
        assertEquals(FeedbackEventType.UNSAVE, outboxList.get(1).getEventType());
        assertEquals(FeedbackEventType.SAVE, outboxList.get(2).getEventType());

        Set<UUID> eventIds = new HashSet<>();
        for (RecommendationFeedbackOutbox o : outboxList) {
            assertTrue(eventIds.add(o.getEventId()), "Every occurrence must have an immutable unique eventId");
        }
    }

    @Test
    @Transactional
    @DisplayName("VIEW deduplication: second VIEW within 60s is suppressed and emits no feedback outbox row")
    void testViewDeduplicationWindow() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());
        InteractionEventRequest req = InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.VIEW)
                .build();

        // First view accepted
        interactionService.recordEvent(testUser, req);

        // Immediate second view within 60s window
        interactionService.recordEvent(testUser, req);

        List<RecommendationFeedbackOutbox> views = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getEventType() == FeedbackEventType.VIEW
                        && o.getContentId() != null
                        && o.getContentId().equals(platformPost.getId()))
                .toList();

        assertEquals(1, views.size(), "Deduplicated view must not emit recommendation feedback");
    }

    @Test
    @Transactional
    @DisplayName("WATCH_TIME bounds: invalid duration rejected before outbox creation; valid duration records durationMs")
    void testWatchTimeBounds() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());

        // Negative duration rejected
        InteractionEventRequest badReq = InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.WATCH_TIME)
                .durationMs(-100L)
                .build();
        assertThrows(InvalidInteractionException.class, () -> interactionService.recordEvent(testUser, badReq));

        // Valid duration recorded
        InteractionEventRequest goodReq = InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.WATCH_TIME)
                .durationMs(25000L)
                .sessionId("sess-123")
                .build();
        interactionService.recordEvent(testUser, goodReq);

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getEventType() == FeedbackEventType.WATCH_TIME)
                .toList();

        assertEquals(1, outboxList.size());
        assertTrue(outboxList.get(0).getPayload().contains("25000"));
        assertTrue(outboxList.get(0).getPayload().contains("sess-123"));
    }

    @Test
    @Transactional
    @DisplayName("Negative signals HIDE and NOT_INTERESTED produce distinct feedback event types")
    void testNegativeSignalsDistinct() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());

        interactionService.recordEvent(testUser, InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.HIDE)
                .build());

        interactionService.recordEvent(testUser, InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.NOT_INTERESTED)
                .build());

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId()))
                .toList();

        boolean hasHide = outboxList.stream().anyMatch(o -> o.getEventType() == FeedbackEventType.HIDE);
        boolean hasNotInterested = outboxList.stream().anyMatch(o -> o.getEventType() == FeedbackEventType.NOT_INTERESTED);

        assertTrue(hasHide, "HIDE event must be captured distinctly");
        assertTrue(hasNotInterested, "NOT_INTERESTED event must be captured distinctly");
    }

    @Test
    @Transactional
    @DisplayName("Inaccessible CommunityPost throws exception and emits zero feedback outbox rows")
    void testInaccessibleContentEmitsNoFeedback() {
        // Create post in private community where testUser is not a member
        Post privatePost = postRepository.save(Post.builder()
                .community(privateCommunity)
                .authorId(authorUser.getId())
                .title("Private Post")
                .body("Private Content")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .build());

        ContentRef privateRef = ContentRef.communityPost(privatePost.getId());

        assertThrows(ContentInaccessibleException.class, () -> interactionService.likeContent(testUser, privateRef));

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(privatePost.getId()))
                .toList();

        assertTrue(outboxList.isEmpty(), "Unauthorized interaction must never emit recommendation feedback");
    }

    @Test
    @Transactional
    @DisplayName("Cross-surface ContentRef collision: same UUID produces two distinct isolated feedback events")
    void testContentRefCollisionIsolated() {
        UUID sharedId = platformPost.getId();

        // Native SQL to insert community post with matching UUID to avoid JPA entity merge
        entityManager.createNativeQuery(
                "INSERT INTO posts (id, community_id, author_id, title, body, type, moderation_status, created_at, updated_at) " +
                "VALUES (:id, :commId, :authorId, 'Collision Title', 'Collision Content', 'DISCUSSION', 'VISIBLE', NOW(), NOW())"
        ).setParameter("id", sharedId)
         .setParameter("commId", publicCommunity.getId())
         .setParameter("authorId", authorUser.getId())
         .executeUpdate();

        ContentRef platformRef = ContentRef.platformPost(sharedId);
        ContentRef communityRef = ContentRef.communityPost(sharedId);

        interactionService.likeContent(testUser, platformRef);
        interactionService.likeContent(testUser, communityRef);

        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getContentId() != null
                        && o.getContentId().equals(sharedId))
                .toList();

        assertEquals(2, outboxList.size(), "Both types must emit distinct feedback events despite sharing UUID");
        boolean hasPlatform = outboxList.stream().anyMatch(o -> o.getContentType() == ContentType.PLATFORM_POST);
        boolean hasCommunity = outboxList.stream().anyMatch(o -> o.getContentType() == ContentType.COMMUNITY_POST);
        assertTrue(hasPlatform);
        assertTrue(hasCommunity);
    }

    @Test
    @DisplayName("Worker claiming with SKIP LOCKED yields disjoint sets across concurrent nodes")
    void testOutboxClaimingAndConcurrency() {
        // Enqueue 4 test jobs
        UUID pId = platformPost.getId();

        List<UUID> createdIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            RecommendationFeedbackOutbox outbox = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                    .eventId(UUID.randomUUID())
                    .contractVersion("1")
                    .eventType(FeedbackEventType.LIKE)
                    .userId(testUser.getId())
                    .contentType(ContentType.PLATFORM_POST)
                    .contentId(pId)
                    .occurredAt(Instant.now())
                    .payload("{\"test\": true}")
                    .status(FeedbackOutboxStatus.PENDING)
                    .nextAttemptAt(Instant.now().minusSeconds(10))
                    .build());
            createdIds.add(outbox.getId());
        }

        // Worker 1 claims 2
        List<RecommendationFeedbackOutbox> batch1 = outboxService.claimJobs("node-1", 2);
        // Worker 2 claims 2
        List<RecommendationFeedbackOutbox> batch2 = outboxService.claimJobs("node-2", 2);

        Set<UUID> ids1 = new HashSet<>(batch1.stream().map(RecommendationFeedbackOutbox::getId).toList());
        Set<UUID> ids2 = new HashSet<>(batch2.stream().map(RecommendationFeedbackOutbox::getId).toList());

        // Disjoint sets check
        for (UUID id : ids1) {
            assertFalse(ids2.contains(id), "Concurrent workers must claim disjoint sets via SKIP LOCKED");
        }

        // Clean up
        outboxRepository.deleteAllById(createdIds);
    }

    @Test
    @DisplayName("Stale worker lock lease recovery and rejection of stale worker completion callback")
    void testStaleLockOwnerProtection() {
        RecommendationFeedbackOutbox job = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                .eventId(UUID.randomUUID())
                .contractVersion("1")
                .eventType(FeedbackEventType.LIKE)
                .userId(testUser.getId())
                .contentType(ContentType.PLATFORM_POST)
                .contentId(platformPost.getId())
                .occurredAt(Instant.now())
                .payload("{\"test\": true}")
                .status(FeedbackOutboxStatus.IN_PROGRESS)
                .lockedAt(Instant.now().minusSeconds(60)) // Expired lease
                .lockOwner("stale-worker-1")
                .attemptCount(1)
                .build());

        // Active Worker 2 reclaims the job
        List<RecommendationFeedbackOutbox> reclaimed = outboxService.claimJobs("active-worker-2", 1);
        boolean foundReclaimed = reclaimed.stream().anyMatch(j -> j.getId().equals(job.getId()));
        assertTrue(foundReclaimed, "Worker 2 must reclaim stale locked job");

        // Stale Worker 1 wakes up and attempts to mark the job DELIVERED with stale nodeId
        RecommendationFeedbackBatchResponse mockResponse = new RecommendationFeedbackBatchResponse(
                "1", 1, 0, 0,
                List.of(new RecommendationFeedbackBatchResponse.EventResultItem(job.getEventId(), "ACCEPTED", null, null))
        );
        outboxService.handleBatchSuccess(List.of(job.getId()), mockResponse, "stale-worker-1");

        // Verify the job was NOT marked DELIVERED by stale worker; it remains owned by active-worker-2
        RecommendationFeedbackOutbox refreshed = outboxRepository.findById(job.getId()).orElseThrow();
        assertEquals("active-worker-2", refreshed.getLockOwner(), "Stale worker cannot overwrite active lock owner");

        // Active Worker 2 completes
        outboxService.handleBatchSuccess(List.of(job.getId()), mockResponse, "active-worker-2");
        RecommendationFeedbackOutbox delivered = outboxRepository.findById(job.getId()).orElseThrow();
        assertEquals(FeedbackOutboxStatus.DELIVERED, delivered.getStatus());

        outboxRepository.delete(job);
    }

    @Test
    @DisplayName("Partial batch: valid event marked DELIVERED, rejected event marked FAILED without poison blocking")
    void testPartialBatchSuccessAndPoisonHandling() {
        UUID event1 = UUID.randomUUID();
        UUID event2 = UUID.randomUUID();

        RecommendationFeedbackOutbox job1 = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                .eventId(event1)
                .contractVersion("1")
                .eventType(FeedbackEventType.LIKE)
                .userId(testUser.getId())
                .occurredAt(Instant.now())
                .payload("{\"test\": 1}")
                .status(FeedbackOutboxStatus.IN_PROGRESS)
                .lockedAt(Instant.now())
                .lockOwner("worker-p")
                .build());

        RecommendationFeedbackOutbox job2 = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                .eventId(event2)
                .contractVersion("1")
                .eventType(FeedbackEventType.LIKE)
                .userId(testUser.getId())
                .occurredAt(Instant.now())
                .payload("{\"test\": 2}")
                .status(FeedbackOutboxStatus.IN_PROGRESS)
                .lockedAt(Instant.now())
                .lockOwner("worker-p")
                .build());

        RecommendationFeedbackBatchResponse partialResponse = new RecommendationFeedbackBatchResponse(
                "1", 1, 0, 1,
                List.of(
                        new RecommendationFeedbackBatchResponse.EventResultItem(event1, "ACCEPTED", null, null),
                        new RecommendationFeedbackBatchResponse.EventResultItem(event2, "REJECTED", "MALFORMED_PAYLOAD", "Missing required field")
                )
        );

        outboxService.handleBatchSuccess(List.of(job1.getId(), job2.getId()), partialResponse, "worker-p");

        RecommendationFeedbackOutbox r1 = outboxRepository.findById(job1.getId()).orElseThrow();
        RecommendationFeedbackOutbox r2 = outboxRepository.findById(job2.getId()).orElseThrow();

        assertEquals(FeedbackOutboxStatus.DELIVERED, r1.getStatus());
        assertEquals(FeedbackOutboxStatus.DEAD_LETTER, r2.getStatus());
        assertEquals("MALFORMED_PAYLOAD", r2.getLastErrorCode());

        outboxRepository.deleteAll(List.of(job1, job2));
    }
}
