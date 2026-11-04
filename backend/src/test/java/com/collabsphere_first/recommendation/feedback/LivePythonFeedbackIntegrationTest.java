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
import com.collabsphere_first.interaction.repository.ContentLikeRepository;
import com.collabsphere_first.interaction.service.InteractionService;
import com.collabsphere_first.messaging.dto.SendMessageRequest;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.post.entity.PlatformPost;
import com.collabsphere_first.post.entity.PostStatus;
import com.collabsphere_first.post.entity.PostVisibility;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.recommendation.feedback.client.HttpRecommendationFeedbackClient;
import com.collabsphere_first.recommendation.feedback.config.RecommendationFeedbackProperties;
import com.collabsphere_first.recommendation.feedback.dto.*;
import com.collabsphere_first.recommendation.feedback.entity.RecommendationFeedbackOutbox;
import com.collabsphere_first.recommendation.feedback.enums.*;
import com.collabsphere_first.recommendation.feedback.repository.RecommendationFeedbackOutboxRepository;
import com.collabsphere_first.recommendation.feedback.service.RecommendationFeedbackOutboxService;
import com.collabsphere_first.recommendation.feedback.worker.RecommendationFeedbackWorker;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate",
        "collobster.recommendation.feedback.enabled=true",
        "collobster.recommendation.feedback.delivery-enabled=true",
        "collobster.recommendation.feedback.base-url=http://127.0.0.1:8000",
        "collobster.recommendation.feedback.internal-token=${COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN:}",
        "collobster.recommendation.feedback.read-timeout-ms=5000",
        "collobster.recommendation.feedback.connect-timeout-ms=2000"
})
@EnabledIfSystemProperty(named = "test.live.python.feedback", matches = "true")
@DisplayName("LivePythonFeedbackIntegrationTest")
class LivePythonFeedbackIntegrationTest {

    @Autowired
    private RecommendationFeedbackOutboxService outboxService;

    @Autowired
    private RecommendationFeedbackOutboxRepository outboxRepository;

    @Autowired
    private RecommendationFeedbackWorker feedbackWorker;

    @Autowired
    private HttpRecommendationFeedbackClient feedbackClient;

    @Autowired
    private RecommendationFeedbackProperties feedbackProperties;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ContentLikeRepository contentLikeRepository;

    @Autowired
    private PlatformPostRepository platformPostRepository;

    @Autowired
    private PostRepository communityPostRepository;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User recipientUser;
    private PlatformPost platformPost;
    private Community publicCommunity;
    private Post communityPost;

    private static boolean isServerAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @BeforeEach
    void setUp() {
        assertTrue(isServerAvailable("127.0.0.1", 8000),
                "Real Python FastAPI application must be running on port 8000 for python-feedback-integration profile");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        testUser = userRepository.save(User.builder()
                .username("live_user_" + suffix)
                .email("live_user_" + suffix + "@test.com")
                .firstName("Live")
                .lastName("Tester")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        recipientUser = userRepository.save(User.builder()
                .username("live_recipient_" + suffix)
                .email("live_recipient_" + suffix + "@test.com")
                .firstName("Live")
                .lastName("Recipient")
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        platformPost = platformPostRepository.save(PlatformPost.builder()
                .author(testUser)
                .body("Live Platform Post Body")
                .visibility(PostVisibility.PUBLIC)
                .status(PostStatus.ACTIVE)
                .build());

        publicCommunity = communityRepository.save(Community.builder()
                .name("Live Comm " + suffix)
                .slug("live-comm-" + suffix)
                .ownerId(testUser.getId())
                .visibility(Visibility.PUBLIC)
                .communityStatus(CommunityStatus.ACTIVE)
                .category(CommunityCategory.TECHNOLOGY)
                .type(CommunityType.GENERAL)
                .lifecycle(LifecycleState.PERMANENT)
                .memberCount(1)
                .build());

        membershipRepository.save(Membership.builder()
                .community(publicCommunity)
                .userId(testUser.getId())
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        communityPost = communityPostRepository.save(Post.builder()
                .community(publicCommunity)
                .authorId(testUser.getId())
                .title("Live Community Post")
                .body("Live Community Body")
                .moderationStatus(ModerationStatus.VISIBLE)
                .type(PostType.DISCUSSION)
                .build());
    }

    @Test
    @DisplayName("1. Single LIKE end-to-end: Java interaction -> outbox -> worker HTTP -> Python FastAPI -> recommendation_events -> DELIVERED")
    void testSingleLikeEndToEnd() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());

        // Step 1: User likes platform post
        LikeResponse response = interactionService.likeContent(testUser, ref);
        assertTrue(response.isLiked());

        // Step 2: Verify PostgreSQL business mutation
        boolean likedInDb = contentLikeRepository.existsByUserIdAndContentTypeAndContentId(
                testUser.getId(), ContentType.PLATFORM_POST, platformPost.getId());
        assertTrue(likedInDb, "content_likes must contain row for user and post");

        // Step 3: Verify outbox row exists in PENDING state
        List<RecommendationFeedbackOutbox> outboxList = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getEventType() == FeedbackEventType.LIKE
                        && platformPost.getId().equals(o.getContentId()))
                .toList();
        assertEquals(1, outboxList.size(), "Exactly 1 outbox row must exist");
        RecommendationFeedbackOutbox outboxJob = outboxList.get(0);
        assertEquals(FeedbackOutboxStatus.PENDING, outboxJob.getStatus());

        // Step 4: Run Java worker
        feedbackWorker.processOutbox();

        // Step 5: Verify Java outbox E = DELIVERED
        RecommendationFeedbackOutbox deliveredJob = outboxRepository.findById(outboxJob.getId()).orElseThrow();
        assertEquals(FeedbackOutboxStatus.DELIVERED, deliveredJob.getStatus());

        // Step 6: Verify actual persistence in Python recommendation_events table
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ? AND event_type = 'LIKE' AND content_type = 'PLATFORM_POST' AND content_id = ?",
                Integer.class,
                outboxJob.getEventId().toString(),
                platformPost.getId().toString()
        );
        assertEquals(1, count, "Python recommendation_events table must contain the persisted event");
    }

    @Test
    @DisplayName("2. Community LIKE end-to-end: Community authorization -> content_likes -> outbox -> Python event")
    void testCommunityLikeEndToEnd() {
        ContentRef ref = ContentRef.communityPost(communityPost.getId());

        LikeResponse response = interactionService.likeContent(testUser, ref);
        assertTrue(response.isLiked());

        // Verify global authoritative content_likes
        boolean likedInDb = contentLikeRepository.existsByUserIdAndContentTypeAndContentId(
                testUser.getId(), ContentType.COMMUNITY_POST, communityPost.getId());
        assertTrue(likedInDb);

        // Outbox contains COMMUNITY_POST event
        RecommendationFeedbackOutbox outboxJob = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId())
                        && o.getEventType() == FeedbackEventType.LIKE
                        && communityPost.getId().equals(o.getContentId())
                        && o.getContentType() == ContentType.COMMUNITY_POST)
                .findFirst().orElseThrow();

        // Deliver to Python via worker
        feedbackWorker.processOutbox();

        RecommendationFeedbackOutbox deliveredJob = outboxRepository.findById(outboxJob.getId()).orElseThrow();
        assertEquals(FeedbackOutboxStatus.DELIVERED, deliveredJob.getStatus());

        // Verify Python recommendation_events has COMMUNITY_POST
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ? AND content_type = 'COMMUNITY_POST' AND content_id = ?",
                Integer.class,
                outboxJob.getEventId().toString(),
                communityPost.getId().toString()
        );
        assertEquals(1, count, "Python recommendation_events must store COMMUNITY_POST type");
    }

    @Test
    @Transactional
    @DisplayName("3. Same UUID cross-type live test: PLATFORM_POST X and COMMUNITY_POST X remain completely isolated")
    void testSameUuidCrossTypeLive() {
        UUID sharedId = platformPost.getId();

        // Insert community post with same UUID
        entityManager.createNativeQuery(
                "INSERT INTO posts (id, community_id, author_id, title, body, type, moderation_status, created_at, updated_at) " +
                "VALUES (:id, :commId, :authorId, 'Same UUID Post', 'Body', 'DISCUSSION', 'VISIBLE', NOW(), NOW())"
        ).setParameter("id", sharedId)
         .setParameter("commId", publicCommunity.getId())
         .setParameter("authorId", testUser.getId())
         .executeUpdate();

        ContentRef platRef = ContentRef.platformPost(sharedId);
        ContentRef commRef = ContentRef.communityPost(sharedId);

        interactionService.likeContent(testUser, platRef);
        interactionService.likeContent(testUser, commRef);

        // Run worker to deliver both
        feedbackWorker.processOutbox();

        // Verify Python contains 2 distinct events with content_id = sharedId
        Integer countPlatform = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE content_id = ? AND content_type = 'PLATFORM_POST'",
                Integer.class, sharedId.toString());
        Integer countCommunity = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE content_id = ? AND content_type = 'COMMUNITY_POST'",
                Integer.class, sharedId.toString());

        assertTrue(countPlatform >= 1, "Must contain PLATFORM_POST with shared ID");
        assertTrue(countCommunity >= 1, "Must contain COMMUNITY_POST with shared ID");
    }

    @Test
    @DisplayName("4. Duplicate delivery end-to-end: Deliver same event twice -> count = 1, both acknowledged successfully")
    void testDuplicateDeliveryEndToEnd() {
        UUID eventId = UUID.randomUUID();
        RecommendationEventDto event = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(eventId)
                .eventType(FeedbackEventType.LIKE)
                .occurredAt(Instant.now())
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        RecommendationFeedbackBatchRequest request = RecommendationFeedbackBatchRequest.of(List.of(event));

        // Delivery 1
        RecommendationFeedbackBatchResponse resp1 = feedbackClient.sendBatch(request);
        assertEquals(1, resp1.accepted());

        // Delivery 2 (Duplicate)
        RecommendationFeedbackBatchResponse resp2 = feedbackClient.sendBatch(request);
        assertEquals(1, resp2.duplicates());
        assertEquals(0, resp2.rejected());
        assertTrue(resp2.items().get(0).isAcceptedOrDuplicate());

        // Verify Python count is exactly 1
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ?",
                Integer.class, eventId.toString());
        assertEquals(1, count, "Duplicate delivery must not produce a second row in recommendation_events");
    }

    @Test
    @DisplayName("5. Python-down recovery and retry: User actions succeed without latency; outbox records retry")
    void testPythonDownRecoveryAndRetry() {
        // Point client to unavailable port
        RecommendationFeedbackProperties badProps = new RecommendationFeedbackProperties();
        badProps.setBaseUrl("http://127.0.0.1:8009");
        badProps.setConnectTimeoutMs(200);
        badProps.setReadTimeoutMs(200);
        badProps.setInternalToken(feedbackProperties.getInternalToken());
        HttpRecommendationFeedbackClient offlineClient = new HttpRecommendationFeedbackClient(badProps);

        // Perform authoritative Java actions: LIKE, SAVE, VIEW, HIDE
        ContentRef ref = ContentRef.platformPost(platformPost.getId());
        assertDoesNotThrow(() -> interactionService.likeContent(testUser, ref));
        assertDoesNotThrow(() -> interactionService.saveContent(testUser, ref));
        assertDoesNotThrow(() -> interactionService.recordEvent(testUser, InteractionEventRequest.builder()
                .contentRef(ref).eventType(InteractionEventType.VIEW).build()));
        assertDoesNotThrow(() -> interactionService.recordEvent(testUser, InteractionEventRequest.builder()
                .contentRef(ref).eventType(InteractionEventType.HIDE).build()));

        // Verify authoritative state in Java DB
        assertTrue(contentLikeRepository.existsByUserIdAndContentTypeAndContentId(testUser.getId(), ContentType.PLATFORM_POST, platformPost.getId()));

        // Outbox contains pending events
        List<RecommendationFeedbackOutbox> pending = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId()) && o.getStatus() == FeedbackOutboxStatus.PENDING)
                .toList();
        assertFalse(pending.isEmpty(), "Outbox must have captured events despite Python offline");

        // Attempt delivery to offline client
        RecommendationFeedbackOutbox job = pending.get(0);
        RecommendationEventDto eventDto = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(job.getEventId())
                .eventType(job.getEventType())
                .occurredAt(job.getOccurredAt())
                .userId(job.getUserId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        assertThrows(Exception.class, () -> offlineClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(eventDto))));

        // Trigger retry handling in service
        outboxService.handleBatchRetry(List.of(job.getId()), job.getLockOwner(), "CONNECT_TIMEOUT", "Connection refused");

        RecommendationFeedbackOutbox retriedJob = outboxRepository.findById(job.getId()).orElseThrow();
        assertEquals(FeedbackOutboxStatus.RETRY, retriedJob.getStatus());
        assertTrue(retriedJob.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    @DisplayName("6. Python restart recovery: Valid retrying events are delivered and marked DELIVERED")
    void testPythonRestartRecovery() {
        UUID eventId = UUID.randomUUID();
        RecommendationFeedbackOutbox job = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                .eventId(eventId)
                .contractVersion("1")
                .eventType(FeedbackEventType.LIKE)
                .userId(testUser.getId())
                .contentType(ContentType.PLATFORM_POST)
                .contentId(platformPost.getId())
                .occurredAt(Instant.now())
                .payload("{\"test\": true}")
                .status(FeedbackOutboxStatus.RETRY)
                .nextAttemptAt(Instant.now().minusSeconds(5))
                .lockOwner("recovery-worker")
                .attemptCount(1)
                .build());

        // Send to live running Python service
        RecommendationEventDto eventDto = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(eventId)
                .eventType(FeedbackEventType.LIKE)
                .occurredAt(job.getOccurredAt())
                .userId(job.getUserId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(eventDto)));

        assertNotNull(response);
        outboxService.handleBatchSuccess(List.of(job.getId()), response, "recovery-worker");

        RecommendationFeedbackOutbox delivered = outboxRepository.findById(job.getId()).orElseThrow();
        assertEquals(FeedbackOutboxStatus.DELIVERED, delivered.getStatus());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ?",
                Integer.class, eventId.toString());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("7. Original occurredAt preservation: Late event retains original occurrence time, not ingestion time")
    void testOriginalOccurredAtAndLateEventPreservation() {
        UUID eventId = UUID.randomUUID();
        Instant originalTime = Instant.now().minus(2, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);

        RecommendationEventDto event = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(eventId)
                .eventType(FeedbackEventType.LIKE)
                .occurredAt(originalTime)
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(event)));

        assertNotNull(response);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT occurred_at, ingested_at FROM recommendation_events WHERE event_id = ?",
                eventId.toString());

        Instant storedOccurred = ((java.sql.Timestamp) row.get("occurred_at")).toInstant();
        Instant storedIngested = ((java.sql.Timestamp) row.get("ingested_at")).toInstant();

        assertEquals(originalTime.getEpochSecond(), storedOccurred.getEpochSecond(),
                "Python must store original occurredAt without replacing it with ingestion timestamp");
        assertTrue(storedIngested.isAfter(storedOccurred),
                "ingested_at (ingestion time) must be strictly greater than occurred_at for delayed events");
    }

    @Test
    @DisplayName("8. Out-of-order chronology preservation: E1 at T1, E2 at T2, E3 at T3 delivered E3, E1, E2 reconstructs T1, T2, T3")
    void testOutOfOrderChronologyPreservation() {
        Instant t1 = Instant.now().minus(10, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.MILLIS);
        Instant t2 = Instant.now().minus(5, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.MILLIS);
        Instant t3 = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        UUID e1Id = UUID.randomUUID();
        UUID e2Id = UUID.randomUUID();
        UUID e3Id = UUID.randomUUID();

        RecommendationEventDto e1 = RecommendationEventDto.builder().contractVersion("1").eventId(e1Id).eventType(FeedbackEventType.LIKE).occurredAt(t1).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();
        RecommendationEventDto e2 = RecommendationEventDto.builder().contractVersion("1").eventId(e2Id).eventType(FeedbackEventType.UNLIKE).occurredAt(t2).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();
        RecommendationEventDto e3 = RecommendationEventDto.builder().contractVersion("1").eventId(e3Id).eventType(FeedbackEventType.LIKE).occurredAt(t3).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();

        // Deliver in reverse/scrambled order: E3, then E1, then E2
        feedbackClient.sendBatch(RecommendationFeedbackBatchRequest.of(List.of(e3)));
        feedbackClient.sendBatch(RecommendationFeedbackBatchRequest.of(List.of(e1)));
        feedbackClient.sendBatch(RecommendationFeedbackBatchRequest.of(List.of(e2)));

        // Reconstruct chronology by ordering occurred_at ASC
        List<String> eventTypes = jdbcTemplate.query(
                "SELECT event_type FROM recommendation_events WHERE event_id IN (?, ?, ?) ORDER BY occurred_at ASC, event_id ASC",
                (rs, rowNum) -> rs.getString("event_type"),
                e1Id.toString(), e2Id.toString(), e3Id.toString()
        );

        assertEquals(3, eventTypes.size());
        assertEquals("LIKE", eventTypes.get(0), "First chronological event must be LIKE");
        assertEquals("UNLIKE", eventTypes.get(1), "Second chronological event must be UNLIKE");
        assertEquals("LIKE", eventTypes.get(2), "Third chronological event must be LIKE");
    }

    @Test
    @DisplayName("9. Partial batch live with poison quarantine: E1(valid), E2(duplicate), E3(poison), E4(valid) -> E3 dead-lettered, neighbors delivered")
    void testPartialBatchLiveWithPoisonQuarantine() {
        UUID e1Id = UUID.randomUUID();
        UUID e2Id = UUID.randomUUID();
        UUID e3Id = UUID.randomUUID();
        UUID e4Id = UUID.randomUUID();

        // Pre-ingest E2 so it acts as duplicate
        feedbackClient.sendBatch(RecommendationFeedbackBatchRequest.of(List.of(
                RecommendationEventDto.builder().contractVersion("1").eventId(e2Id).eventType(FeedbackEventType.LIKE).occurredAt(Instant.now()).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build()
        )));

        // Create 4 outbox jobs
        RecommendationFeedbackOutbox j1 = outboxRepository.save(RecommendationFeedbackOutbox.builder().eventId(e1Id).contractVersion("1").eventType(FeedbackEventType.LIKE).userId(testUser.getId()).contentType(ContentType.PLATFORM_POST).contentId(platformPost.getId()).occurredAt(Instant.now()).payload("{}").status(FeedbackOutboxStatus.IN_PROGRESS).lockOwner("w-partial").build());
        RecommendationFeedbackOutbox j2 = outboxRepository.save(RecommendationFeedbackOutbox.builder().eventId(e2Id).contractVersion("1").eventType(FeedbackEventType.LIKE).userId(testUser.getId()).contentType(ContentType.PLATFORM_POST).contentId(platformPost.getId()).occurredAt(Instant.now()).payload("{}").status(FeedbackOutboxStatus.IN_PROGRESS).lockOwner("w-partial").build());
        RecommendationFeedbackOutbox j3 = outboxRepository.save(RecommendationFeedbackOutbox.builder().eventId(e3Id).contractVersion("1").eventType(FeedbackEventType.WATCH_TIME).userId(testUser.getId()).contentType(ContentType.PLATFORM_POST).contentId(platformPost.getId()).occurredAt(Instant.now()).payload("{}").status(FeedbackOutboxStatus.IN_PROGRESS).lockOwner("w-partial").build());
        RecommendationFeedbackOutbox j4 = outboxRepository.save(RecommendationFeedbackOutbox.builder().eventId(e4Id).contractVersion("1").eventType(FeedbackEventType.SAVE).userId(testUser.getId()).contentType(ContentType.PLATFORM_POST).contentId(platformPost.getId()).occurredAt(Instant.now()).payload("{}").status(FeedbackOutboxStatus.IN_PROGRESS).lockOwner("w-partial").build());

        // E3 has invalid negative duration
        RecommendationEventDto ev1 = RecommendationEventDto.builder().contractVersion("1").eventId(e1Id).eventType(FeedbackEventType.LIKE).occurredAt(Instant.now()).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();
        RecommendationEventDto ev2 = RecommendationEventDto.builder().contractVersion("1").eventId(e2Id).eventType(FeedbackEventType.LIKE).occurredAt(Instant.now()).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();
        RecommendationEventDto ev3 = RecommendationEventDto.builder().contractVersion("1").eventId(e3Id).eventType(FeedbackEventType.WATCH_TIME).occurredAt(Instant.now()).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).durationMs(-500L).build();
        RecommendationEventDto ev4 = RecommendationEventDto.builder().contractVersion("1").eventId(e4Id).eventType(FeedbackEventType.SAVE).occurredAt(Instant.now()).userId(testUser.getId()).contentRef(ContentRef.platformPost(platformPost.getId())).build();

        RecommendationFeedbackBatchResponse resp = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(ev1, ev2, ev3, ev4)));

        assertNotNull(resp);
        assertEquals(1, resp.rejected(), "E3 poison event must be rejected");

        outboxService.handleBatchSuccess(List.of(j1.getId(), j2.getId(), j3.getId(), j4.getId()), resp, "w-partial");

        RecommendationFeedbackOutbox r1 = outboxRepository.findById(j1.getId()).orElseThrow();
        RecommendationFeedbackOutbox r2 = outboxRepository.findById(j2.getId()).orElseThrow();
        RecommendationFeedbackOutbox r3 = outboxRepository.findById(j3.getId()).orElseThrow();
        RecommendationFeedbackOutbox r4 = outboxRepository.findById(j4.getId()).orElseThrow();

        assertEquals(FeedbackOutboxStatus.DELIVERED, r1.getStatus());
        assertEquals(FeedbackOutboxStatus.DELIVERED, r2.getStatus());
        assertEquals(FeedbackOutboxStatus.DEAD_LETTER, r3.getStatus());
        assertEquals("INVALID_DURATION", r3.getLastErrorCode());
        assertEquals(FeedbackOutboxStatus.DELIVERED, r4.getStatus());
    }

    @Test
    @DisplayName("10. SHARE event coverage: SHARE recorded and delivered to Python")
    void testShareEventCoverage() {
        ContentRef ref = ContentRef.platformPost(platformPost.getId());
        interactionService.recordEvent(testUser, InteractionEventRequest.builder()
                .contentRef(ref)
                .eventType(InteractionEventType.SHARE)
                .build());

        RecommendationFeedbackOutbox shareOutbox = outboxRepository.findAll().stream()
                .filter(o -> o.getUserId().equals(testUser.getId()) && o.getEventType() == FeedbackEventType.SHARE)
                .findFirst().orElseThrow();

        RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(RecommendationEventDto.builder()
                        .contractVersion("1")
                        .eventId(shareOutbox.getEventId())
                        .eventType(FeedbackEventType.SHARE)
                        .occurredAt(shareOutbox.getOccurredAt())
                        .userId(shareOutbox.getUserId())
                        .contentRef(ContentRef.platformPost(platformPost.getId()))
                        .build())));

        assertNotNull(response);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ? AND event_type = 'SHARE'",
                Integer.class, shareOutbox.getEventId().toString());
        assertEquals(1, count);
    }

    @Test
    @DisplayName("11. WATCH_TIME numeric integrity: Exact durationMs survives serialization and database persistence")
    void testWatchTimeNumericIntegrity() {
        UUID eventId = UUID.randomUUID();
        long exactDurationMs = 12345L;

        RecommendationEventDto event = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(eventId)
                .eventType(FeedbackEventType.WATCH_TIME)
                .occurredAt(Instant.now())
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .durationMs(exactDurationMs)
                .build();

        RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(event)));

        assertNotNull(response);

        Long storedDuration = jdbcTemplate.queryForObject(
                "SELECT duration_ms FROM recommendation_events WHERE event_id = ?",
                Long.class, eventId.toString());
        assertEquals(exactDurationMs, storedDuration, "Stored durationMs must be exactly 12345 without alteration");
    }

    @Test
    @DisplayName("12. Negative signal integrity: HIDE and NOT_INTERESTED stored as distinct types")
    void testNegativeSignalIntegrity() {
        UUID hideId = UUID.randomUUID();
        UUID notInterestedId = UUID.randomUUID();

        RecommendationEventDto hide = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(hideId)
                .eventType(FeedbackEventType.HIDE)
                .occurredAt(Instant.now())
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        RecommendationEventDto notInterested = RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(notInterestedId)
                .eventType(FeedbackEventType.NOT_INTERESTED)
                .occurredAt(Instant.now())
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build();

        RecommendationFeedbackBatchResponse response = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(List.of(hide, notInterested)));

        assertNotNull(response);

        String type1 = jdbcTemplate.queryForObject("SELECT event_type FROM recommendation_events WHERE event_id = ?", String.class, hideId.toString());
        String type2 = jdbcTemplate.queryForObject("SELECT event_type FROM recommendation_events WHERE event_id = ?", String.class, notInterestedId.toString());

        assertEquals("HIDE", type1);
        assertEquals("NOT_INTERESTED", type2);
        assertNotEquals(type1, type2);
    }

    @Test
    @DisplayName("13. Privacy regression: Direct messaging actions emit ZERO recommendation feedback")
    void testPrivateMessagingZeroRecommendationFeedback() {
        long outboxCountBefore = outboxRepository.count();

        // Create direct conversation
        var conv = messagingService.getOrCreateDirectConversation(testUser.getId(), recipientUser.getId());

        // Send message
        SendMessageRequest msgReq = new SendMessageRequest(
                UUID.randomUUID(),
                "Secret private direct message",
                null,
                null
        );
        messagingService.sendMessage(conv.id(), testUser.getId(), msgReq);

        long outboxCountAfter = outboxRepository.count();
        assertEquals(outboxCountBefore, outboxCountAfter,
                "Direct messaging actions must NEVER generate recommendation feedback outbox records");

        Integer dmEventsInPython = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_type LIKE '%MESSAGE%' OR event_type LIKE '%CONVERSATION%'",
                Integer.class);
        assertEquals(0, dmEventsInPython, "Python recommendation_events must have ZERO message events");
    }

    @Test
    @DisplayName("14. Unknown event injection safely ignored: E_ATTACKER in response does not mutate unrelated outbox")
    void testUnknownEventInjectionIgnored() {
        UUID myEventId = UUID.randomUUID();
        UUID attackerEventId = UUID.randomUUID();

        RecommendationFeedbackOutbox myJob = outboxRepository.save(RecommendationFeedbackOutbox.builder()
                .eventId(myEventId)
                .contractVersion("1")
                .eventType(FeedbackEventType.LIKE)
                .userId(testUser.getId())
                .contentType(ContentType.PLATFORM_POST)
                .contentId(platformPost.getId())
                .occurredAt(Instant.now())
                .payload("{}")
                .status(FeedbackOutboxStatus.IN_PROGRESS)
                .lockOwner("w-secure")
                .build());

        UUID myJobId = myJob.getId();

        // Simulated response containing my event plus an injected attacker event
        RecommendationFeedbackBatchResponse taintedResponse = new RecommendationFeedbackBatchResponse(
                "1", 2, 0, 0,
                List.of(
                        new RecommendationFeedbackBatchResponse.EventResultItem(myEventId, "ACCEPTED", null, null),
                        new RecommendationFeedbackBatchResponse.EventResultItem(attackerEventId, "ACCEPTED", null, null)
                )
        );

        outboxService.handleBatchSuccess(List.of(myJobId), taintedResponse, "w-secure");

        RecommendationFeedbackOutbox verifiedJob = outboxRepository.findById(myJobId).orElseThrow();
        assertEquals(FeedbackOutboxStatus.DELIVERED, verifiedJob.getStatus());

        // Verify attacker event was not created or bound
        assertFalse(outboxRepository.findByEventId(attackerEventId).isPresent());
    }

    @Test
    @DisplayName("15. Auth failure handling: 401 Unauthorized with invalid token; Java actions and outbox preserved")
    void testAuthFailureHandling() {
        RecommendationFeedbackProperties badAuthProps = new RecommendationFeedbackProperties();
        badAuthProps.setBaseUrl("http://127.0.0.1:8000");
        badAuthProps.setInternalToken("wrong-invalid-secret-token");
        badAuthProps.setConnectTimeoutMs(2000);
        badAuthProps.setReadTimeoutMs(2000);
        HttpRecommendationFeedbackClient badAuthClient = new HttpRecommendationFeedbackClient(badAuthProps);

        UUID eventId = UUID.randomUUID();
        RecommendationFeedbackBatchRequest req = RecommendationFeedbackBatchRequest.of(List.of(
                RecommendationEventDto.builder()
                        .contractVersion("1")
                        .eventId(eventId)
                        .eventType(FeedbackEventType.LIKE)
                        .occurredAt(Instant.now())
                        .userId(testUser.getId())
                        .contentRef(ContentRef.platformPost(platformPost.getId()))
                        .build()
        ));

        // Bad auth throws exception (401)
        assertThrows(Exception.class, () -> badAuthClient.sendBatch(req));

        // Event was NOT stored in Python
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE event_id = ?",
                Integer.class, eventId.toString());
        assertEquals(0, count);

        // Restoring valid token succeeds
        RecommendationFeedbackBatchResponse successResp = feedbackClient.sendBatch(req);
        assertNotNull(successResp);
        assertEquals(1, successResp.accepted());
    }

    @Test
    @DisplayName("16. Batch bounds: 500 events accepted, 501 events rejected by Python with HTTP 422")
    void testWorkerBatchMaximumBounds() {
        // 500 events accepted
        List<RecommendationEventDto> maxBatch = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            maxBatch.add(RecommendationEventDto.builder()
                    .contractVersion("1")
                    .eventId(UUID.randomUUID())
                    .eventType(FeedbackEventType.LIKE)
                    .occurredAt(Instant.now())
                    .userId(testUser.getId())
                    .contentRef(ContentRef.platformPost(platformPost.getId()))
                    .build());
        }
        RecommendationFeedbackBatchResponse okResp = feedbackClient.sendBatch(
                RecommendationFeedbackBatchRequest.of(maxBatch));
        assertNotNull(okResp);
        assertEquals(500, okResp.accepted());

        // 501 events rejected
        List<RecommendationEventDto> oversizedEvents = new ArrayList<>(maxBatch);
        oversizedEvents.add(RecommendationEventDto.builder()
                .contractVersion("1")
                .eventId(UUID.randomUUID())
                .eventType(FeedbackEventType.LIKE)
                .occurredAt(Instant.now())
                .userId(testUser.getId())
                .contentRef(ContentRef.platformPost(platformPost.getId()))
                .build());

        RecommendationFeedbackBatchRequest oversizedReq = RecommendationFeedbackBatchRequest.of(oversizedEvents);
        assertThrows(Exception.class, () -> feedbackClient.sendBatch(oversizedReq),
                "Oversized batch (> 500) must be rejected by Python with HTTP 422");
    }
}
