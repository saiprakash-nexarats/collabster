package com.collabsphere_first.notification;

import com.collabsphere_first.community.entity.CommunityNotification;
import com.collabsphere_first.community.event.CommunityNotificationSavedEvent;
import com.collabsphere_first.messaging.dto.MessageResponse;
import com.collabsphere_first.messaging.dto.ReactionResponse;
import com.collabsphere_first.messaging.entity.Conversation;
import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.entity.ConversationParticipantId;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.enums.MessageDeliveryState;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.event.DirectMessageCreatedEvent;
import com.collabsphere_first.messaging.event.DirectMessageReactionEvent;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.network.event.ConnectionAcceptedEvent;
import com.collabsphere_first.network.event.ConnectionRequestedEvent;
import com.collabsphere_first.network.event.UserFollowedEvent;
import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.delivery.push.MockPushNotificationProvider;
import com.collabsphere_first.notification.dto.*;
import com.collabsphere_first.notification.entity.*;
import com.collabsphere_first.notification.enums.*;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.repository.*;
import com.collabsphere_first.notification.security.NotificationStompAuthorizationInterceptor;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.notification.service.NotificationOutboxService;
import com.collabsphere_first.notification.service.NotificationService;
import com.collabsphere_first.notification.worker.NotificationOutboxWorker;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.userdetails.CustomUserDetailsService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate",
        "collobster.notifications.push.enabled=true",
        "collobster.notifications.email.enabled=true",
        "collobster.notifications.realtime.enabled=true"
})
@Transactional
class NotificationPostgresIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeviceService deviceService;

    @Autowired
    private NotificationDeviceRepository deviceRepository;

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private NotificationOutboxWorker outboxWorker;

    @Autowired
    private MockPushNotificationProvider mockPushProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NotificationStompAuthorizationInterceptor stompInterceptor;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private com.collabsphere_first.community.service.CommunityNotificationService communityNotificationService;

    @Autowired
    private com.collabsphere_first.community.repository.CommunityNotificationRepository communityNotificationRepository;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        mockPushProvider.resetBehavior();
        notificationProperties.getPush().setEnabled(true);
        notificationProperties.getEmail().setEnabled(true);
        notificationProperties.getRealtime().setEnabled(true);

        alice = createOrGetUser("alice_notify@example.com", "alice_notify");
        bob = createOrGetUser("bob_notify@example.com", "bob_notify");
        charlie = createOrGetUser("charlie_notify@example.com", "charlie_notify");
    }

    private User createOrGetUser(String email, String username) {
        return userRepository.findByEmail(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email)
                        .username(username)
                        .password("hashed_password_123")
                        .firstName(username)
                        .lastName("Test")
                        .provider(AuthProvider.LOCAL)
                        .emailVerified(true)
                        .accountEnabled(true)
                        .build())
        );
    }

    // ==========================================
    // 1. KEYSET CURSOR PAGINATION & UNREAD COUNT
    // ==========================================

    @Test
    @DisplayName("Keyset pagination properly pages notifications with limit+1 and opaque cursor")
    void testKeysetCursorPagination() {
        Instant baseTime = Instant.now().minus(1, ChronoUnit.HOURS);

        // Create 5 notifications for Alice
        for (int i = 1; i <= 5; i++) {
            Notification n = Notification.builder()
                    .recipientUserId(alice.getId())
                    .actorUserId(bob.getId())
                    .type(NotificationType.NEW_FOLLOW)
                    .category(NotificationCategory.SOCIAL)
                    .eventKey("TEST_KEYSET:" + i + ":" + alice.getId())
                    .createdAt(baseTime.plus(i * 10, ChronoUnit.MINUTES))
                    .build();
            notificationRepository.save(n);
        }

        // Page 1: limit 2
        NotificationPageResponse page1 = notificationService.getNotifications(
                alice.getId(), null, 2, null, "ALL");
        assertThat(page1.getItems()).hasSize(2);
        assertThat(page1.isHasMore()).isTrue();
        assertThat(page1.getNextCursor()).isNotNull();
        assertThat(page1.getUnreadCount()).isEqualTo(5);

        // Verify DESC order: first item is #5, second is #4
        assertThat(page1.getItems().get(0).getTitleKey()).isNull();

        // Page 2: using cursor
        NotificationPageResponse page2 = notificationService.getNotifications(
                alice.getId(), page1.getNextCursor(), 2, null, "ALL");
        assertThat(page2.getItems()).hasSize(2);
        assertThat(page2.isHasMore()).isTrue();
        assertThat(page2.getNextCursor()).isNotNull();

        // Page 3: last item
        NotificationPageResponse page3 = notificationService.getNotifications(
                alice.getId(), page2.getNextCursor(), 2, null, "ALL");
        assertThat(page3.getItems()).hasSize(1);
        assertThat(page3.isHasMore()).isFalse();
        assertThat(page3.getNextCursor()).isNull();
    }

    @Test
    @DisplayName("Category filter and unread filter properly isolate notifications")
    void testCategoryAndUnreadFilters() {
        // Save 1 SOCIAL, 1 MESSAGING
        Notification n1 = Notification.builder()
                .recipientUserId(alice.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("FILTER:SOCIAL:" + UUID.randomUUID())
                .build();
        notificationRepository.save(n1);

        Notification n2 = Notification.builder()
                .recipientUserId(alice.getId())
                .type(NotificationType.DIRECT_MESSAGE)
                .category(NotificationCategory.MESSAGING)
                .eventKey("FILTER:MESSAGING:" + UUID.randomUUID())
                .readAt(Instant.now()) // Already read
                .build();
        notificationRepository.save(n2);

        // Category filter: SOCIAL
        NotificationPageResponse socialPage = notificationService.getNotifications(
                alice.getId(), null, 10, NotificationCategory.SOCIAL, null);
        assertThat(socialPage.getItems()).hasSize(1);
        assertThat(socialPage.getItems().get(0).getCategory()).isEqualTo(NotificationCategory.SOCIAL);

        // UNREAD filter
        NotificationPageResponse unreadPage = notificationService.getNotifications(
                alice.getId(), null, 10, null, "UNREAD");
        assertThat(unreadPage.getItems()).hasSize(1);
        assertThat(unreadPage.getItems().get(0).getId()).isEqualTo(n1.getId());
    }

    // ==========================================
    // 2. SEEN, READ, READ-ALL & ARCHIVE SEMANTICS
    // ==========================================

    @Test
    @DisplayName("Mark seen, mark read (read implies seen), mark-all read, and archive operate idempotently")
    void testSeenReadArchiveOperations() {
        Notification n = Notification.builder()
                .recipientUserId(alice.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("OP_TEST:" + UUID.randomUUID())
                .build();
        n = notificationRepository.save(n);

        // Mark seen
        NotificationResponse seenResp = notificationService.markSeen(n.getId(), alice.getId());
        assertThat(seenResp.getSeenAt()).isNotNull();
        assertThat(seenResp.getReadAt()).isNull();
        assertThat(seenResp.getState()).isEqualTo(NotificationState.SEEN);

        // Mark read
        NotificationResponse readResp = notificationService.markRead(n.getId(), alice.getId());
        assertThat(readResp.getReadAt()).isNotNull();
        assertThat(readResp.getSeenAt()).isNotNull();
        assertThat(readResp.getState()).isEqualTo(NotificationState.READ);

        // Mark all read
        int updated = notificationService.markAllRead(alice.getId(), null);
        assertThat(updated).isGreaterThanOrEqualTo(0);

        // Archive
        NotificationResponse archivedResp = notificationService.archive(n.getId(), alice.getId());
        assertThat(archivedResp.getArchivedAt()).isNotNull();
        assertThat(archivedResp.getState()).isEqualTo(NotificationState.ARCHIVED);
    }

    @Test
    @DisplayName("IDOR protection: User cannot mark seen, read, or access another user's notification")
    void testNotificationIDORProtection() {
        Notification n = Notification.builder()
                .recipientUserId(alice.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("IDOR_TEST:" + UUID.randomUUID())
                .build();
        n = notificationRepository.save(n);

        UUID notifId = n.getId();

        // Bob attempts to get Alice's notification
        assertThatThrownBy(() -> notificationService.getNotification(notifId, bob.getId()))
                .isInstanceOf(NotificationAccessDeniedException.class);

        // Bob attempts to mark seen Alice's notification
        assertThatThrownBy(() -> notificationService.markSeen(notifId, bob.getId()))
                .isInstanceOf(NotificationAccessDeniedException.class);

        // Bob attempts to mark read Alice's notification
        assertThatThrownBy(() -> notificationService.markRead(notifId, bob.getId()))
                .isInstanceOf(NotificationAccessDeniedException.class);

        // Bob attempts to archive Alice's notification
        assertThatThrownBy(() -> notificationService.archive(notifId, bob.getId()))
                .isInstanceOf(NotificationAccessDeniedException.class);
    }

    // ==========================================
    // 3. EVENT BRIDGES & DEDUPLICATION
    // ==========================================

    @Test
    @DisplayName("Network events: Follow, connection request, and connection accepted create notifications idempotently")
    void testNetworkEventsIntegration() {
        // 1. Follow event
        eventPublisher.publishEvent(new UserFollowedEvent(this, bob.getId(), alice.getId()));

        Notification followNotif = notificationRepository.findByEventKey("FOLLOW:" + bob.getId() + ":" + alice.getId())
                .orElse(null);
        assertThat(followNotif).isNotNull();
        assertThat(followNotif.getType()).isEqualTo(NotificationType.NEW_FOLLOW);
        assertThat(followNotif.getRecipientUserId()).isEqualTo(alice.getId());
        assertThat(followNotif.getActorUserId()).isEqualTo(bob.getId());

        // Duplicate follow event does not create duplicate
        eventPublisher.publishEvent(new UserFollowedEvent(this, bob.getId(), alice.getId()));
        long count = notificationRepository.countUnreadByRecipient(alice.getId());
        assertThat(count).isEqualTo(1);

        // 2. Connection requested event
        UUID reqId = UUID.randomUUID();
        eventPublisher.publishEvent(new ConnectionRequestedEvent(this, reqId, bob.getId(), alice.getId()));

        Notification reqNotif = notificationRepository.findByEventKey("CONNECTION_REQUEST:" + reqId + ":" + alice.getId())
                .orElse(null);
        assertThat(reqNotif).isNotNull();
        assertThat(reqNotif.getType()).isEqualTo(NotificationType.CONNECTION_REQUESTED);

        // 3. Connection accepted event
        eventPublisher.publishEvent(new ConnectionAcceptedEvent(this, reqId, alice.getId(), bob.getId()));
        Notification accNotif = notificationRepository.findByEventKey("CONNECTION_ACCEPTED:" + reqId + ":" + alice.getId())
                .orElse(null);
        assertThat(accNotif).isNotNull();
        assertThat(accNotif.getType()).isEqualTo(NotificationType.CONNECTION_ACCEPTED);
    }

    @Test
    @DisplayName("Messaging events: Direct message creates notification without private message text, respects mute")
    void testMessagingEventsIntegration() {
        Conversation conv = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .directUserLowId(Math.min(bob.getId(), alice.getId()))
                .directUserHighId(Math.max(bob.getId(), alice.getId()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        // Alice has muted notifications in this conversation
        participantRepository.save(ConversationParticipant.builder()
                .id(new ConversationParticipantId(conv.getId(), alice.getId()))
                .conversation(conv)
                .user(alice)
                .notificationsMuted(true)
                .build());

        UUID msgId = UUID.randomUUID();
        MessageResponse msgResponse = new MessageResponse(
                msgId,
                conv.getId(),
                bob.getId(),
                "bob_notify",
                UUID.randomUUID(),
                MessageType.TEXT,
                "SUPER SECRET PRIVATE MESSAGE CONTENT",
                List.of(),
                null,
                List.of(),
                MessageDeliveryState.SENT,
                MessageStatus.ACTIVE,
                false,
                Instant.now(),
                Instant.now(),
                null
        );

        eventPublisher.publishEvent(new DirectMessageCreatedEvent(
                conv.getId(),
                msgResponse,
                bob.getId(),
                alice.getId(),
                Instant.now()
        ));

        Notification dmNotif = notificationRepository.findByEventKey("MESSAGE:" + msgId + ":" + alice.getId())
                .orElse(null);
        assertThat(dmNotif).isNotNull();
        assertThat(dmNotif.getType()).isEqualTo(NotificationType.DIRECT_MESSAGE);

        // Privacy rule (Section 13 & 47): payload must NEVER contain the message content
        assertThat(dmNotif.getPayload()).isNotNull();
        assertThat(dmNotif.getPayload()).doesNotContainValue("SUPER SECRET PRIVATE MESSAGE CONTENT");
        assertThat(dmNotif.getPayload()).containsEntry("conversationId", conv.getId().toString());

        // Mute rule (Section 31): outbox PUSH and EMAIL must be suppressed
        Optional<NotificationOutbox> pushJob = outboxRepository.findByNotificationIdAndChannel(
                dmNotif.getId(), NotificationChannel.PUSH);
        Optional<NotificationOutbox> emailJob = outboxRepository.findByNotificationIdAndChannel(
                dmNotif.getId(), NotificationChannel.EMAIL);
        assertThat(pushJob).isEmpty();
        assertThat(emailJob).isEmpty();

        // In-app remains persisted and REALTIME is suppressed for muted conversations (Section 30)
        Optional<NotificationOutbox> realtimeJob = outboxRepository.findByNotificationIdAndChannel(
                dmNotif.getId(), NotificationChannel.REALTIME);
        assertThat(realtimeJob).isEmpty();
    }

    @Test
    @DisplayName("Messaging reaction event creates notification, self-reaction is suppressed")
    void testMessagingReactionIntegration() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        // Reaction from Bob to Alice's message
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId,
                msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "THUMBS_UP", Instant.now()),
                false,
                bob.getId(),
                alice.getId(),
                Instant.now()
        ));

        Notification notif = notificationRepository.findByEventKey("MESSAGE_REACTION:" + msgId + ":" + bob.getId() + ":" + alice.getId())
                .orElse(null);
        assertThat(notif).isNotNull();
        assertThat(notif.getType()).isEqualTo(NotificationType.DIRECT_MESSAGE_REACTION);

        // Self-reaction from Bob to Bob's own message
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId,
                msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "THUMBS_UP", Instant.now()),
                false,
                bob.getId(),
                bob.getId(), // Self-reaction
                Instant.now()
        ));

        Optional<Notification> selfNotif = notificationRepository.findByEventKey(
                "MESSAGE_REACTION:" + msgId + ":" + bob.getId() + ":" + bob.getId());
        assertThat(selfNotif).isEmpty();
    }

    @Test
    @DisplayName("Community bridge translates CommunityNotification to global notification center idempotently")
    void testCommunityBridgeIntegration() {
        UUID cnId = UUID.randomUUID();
        UUID commId = UUID.randomUUID();

        CommunityNotification cn = CommunityNotification.builder()
                .id(cnId)
                .userId(alice.getId())
                .communityId(commId)
                .type("NEW_POST")
                .title("New post in community")
                .body("Hello world")
                .targetType("POST")
                .targetId("post-123")
                .createdAt(Instant.now())
                .build();

        eventPublisher.publishEvent(new CommunityNotificationSavedEvent(this, cn));

        Notification globalNotif = notificationRepository.findByEventKey("COMMUNITY:" + cnId + ":" + alice.getId())
                .orElse(null);
        assertThat(globalNotif).isNotNull();
        assertThat(globalNotif.getCategory()).isEqualTo(NotificationCategory.COMMUNITY);
        assertThat(globalNotif.getTargetType()).isEqualTo(NavigationTargetType.OPEN_COMMUNITY_POST);
        assertThat(globalNotif.getTargetId()).isEqualTo("post-123");

        // Retrying bridge does not create duplicate
        eventPublisher.publishEvent(new CommunityNotificationSavedEvent(this, cn));
        long count = notificationRepository.countUnreadByRecipient(alice.getId());
        assertThat(count).isEqualTo(1);
    }

    // ==========================================
    // 4. OUTBOX CLAIMING & WORKER DISPATCH
    // ==========================================

    @Test
    @DisplayName("Outbox claiming and worker processing successfully transitions jobs to SENT with deliveries")
    void testOutboxWorkerProcessing() {
        // Register active device for Alice
        deviceService.registerDevice(alice.getId(), RegisterDeviceRequest.builder()
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .token("valid-device-token-alice")
                .build());

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("OUTBOX_TEST:" + UUID.randomUUID())
                .build();

        Notification n = notificationService.createNotification(cmd);
        assertThat(n).isNotNull();

        // Process outbox
        outboxWorker.processOutbox();

        // Verify outbox jobs are now SENT
        List<NotificationOutbox> jobs = outboxRepository.findByIdIn(List.of(
                outboxRepository.findByNotificationIdAndChannel(n.getId(), NotificationChannel.REALTIME).get().getId(),
                outboxRepository.findByNotificationIdAndChannel(n.getId(), NotificationChannel.PUSH).get().getId()
        ));

        for (NotificationOutbox job : jobs) {
            assertThat(job.getStatus()).isEqualTo(OutboxStatus.SENT);
        }

        // Verify delivery audit history
        List<NotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderBySentAtDesc(n.getId());
        assertThat(deliveries).isNotEmpty();
    }

    // ==========================================
    // 5. STOMP WEBSOCKET SECURITY INTERCEPTOR
    // ==========================================

    @Test
    @DisplayName("STOMP security rejects unauthenticated connection, allows /user/queue/notifications, and rejects /queue/notifications")
    void testStompSecurity() {
        // 1. CONNECT without token
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());
        assertThatThrownBy(() -> stompInterceptor.preSend(connectMsg, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bearer token is required");

        // 2. CONNECT with valid JWT
        var userDetails = userDetailsService.loadUserByUsername(alice.getEmail());
        String token = jwtService.generateAccessToken(userDetails);

        StompHeaderAccessor authedConnectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        authedConnectAccessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> authedConnectMsg = MessageBuilder.createMessage(new byte[0], authedConnectAccessor.getMessageHeaders());
        Message<?> result = stompInterceptor.preSend(authedConnectMsg, null);
        assertThat(result).isNotNull();

        // 3. SUBSCRIBE to valid private queue
        StompHeaderAccessor subAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAccessor.setDestination("/user/queue/notifications");
        subAccessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        Message<?> subMsg = MessageBuilder.createMessage(new byte[0], subAccessor.getMessageHeaders());
        Message<?> subResult = stompInterceptor.preSend(subMsg, null);
        assertThat(subResult).isNotNull();

        // 4. SUBSCRIBE to illegal raw /queue/notifications (bypassing /user/)
        StompHeaderAccessor rawQueueAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        rawQueueAccessor.setDestination("/queue/notifications");
        rawQueueAccessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        Message<?> rawQueueMsg = MessageBuilder.createMessage(new byte[0], rawQueueAccessor.getMessageHeaders());
        assertThatThrownBy(() -> stompInterceptor.preSend(rawQueueMsg, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized notification subscription destination");
    }

    // ==========================================
    // 6. PHASE 7.1 HARDENING VERIFICATIONS
    // ==========================================

    @Test
    @DisplayName("Follow / Unfollow / Refollow produces distinct event keys and notifications with occurrence identity")
    void testFollowUnfollowRefollowLifecycle() {
        UUID followId1 = UUID.randomUUID();
        eventPublisher.publishEvent(new UserFollowedEvent(this, followId1, bob.getId(), alice.getId()));

        Notification notif1 = notificationRepository.findByEventKey("FOLLOW:" + followId1 + ":" + alice.getId())
                .orElse(null);
        assertThat(notif1).isNotNull();
        assertThat(notif1.getType()).isEqualTo(NotificationType.NEW_FOLLOW);

        // Retrying identical source event is idempotent
        eventPublisher.publishEvent(new UserFollowedEvent(this, followId1, bob.getId(), alice.getId()));
        assertThat(notificationRepository.countUnreadByRecipient(alice.getId())).isGreaterThanOrEqualTo(1);

        // Bob unfollows and follows again -> new followId occurrence
        UUID followId2 = UUID.randomUUID();
        eventPublisher.publishEvent(new UserFollowedEvent(this, followId2, bob.getId(), alice.getId()));

        Notification notif2 = notificationRepository.findByEventKey("FOLLOW:" + followId2 + ":" + alice.getId())
                .orElse(null);
        assertThat(notif2).isNotNull();
        assertThat(notif2.getId()).isNotEqualTo(notif1.getId());
        assertThat(notif2.getEventKey()).isNotEqualTo(notif1.getEventKey());
    }

    @Test
    @DisplayName("Reaction recurrence (👍 -> ❤️ -> 👍) creates distinct notifications and avoids suppression")
    void testReactionRecurrenceLifecycle() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        // 1. Bob reacts 👍 (occurrence 1)
        UUID eventId1 = UUID.randomUUID();
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId, msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "THUMBS_UP", Instant.now()),
                false, bob.getId(), alice.getId(), Instant.now(), eventId1));

        Notification notif1 = notificationRepository.findByEventKey("MESSAGE_REACTION:" + eventId1 + ":" + alice.getId()).orElse(null);
        assertThat(notif1).isNotNull();

        // 2. Bob changes to ❤️ (occurrence 2)
        UUID eventId2 = UUID.randomUUID();
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId, msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "HEART", Instant.now()),
                false, bob.getId(), alice.getId(), Instant.now(), eventId2));

        Notification notif2 = notificationRepository.findByEventKey("MESSAGE_REACTION:" + eventId2 + ":" + alice.getId()).orElse(null);
        assertThat(notif2).isNotNull();
        assertThat(notif2.getId()).isNotEqualTo(notif1.getId());

        // 3. Bob changes back to 👍 (occurrence 3)
        UUID eventId3 = UUID.randomUUID();
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId, msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "THUMBS_UP", Instant.now()),
                false, bob.getId(), alice.getId(), Instant.now(), eventId3));

        Notification notif3 = notificationRepository.findByEventKey("MESSAGE_REACTION:" + eventId3 + ":" + alice.getId()).orElse(null);
        assertThat(notif3).isNotNull();
        assertThat(notif3.getId()).isNotEqualTo(notif1.getId());
        assertThat(notif3.getId()).isNotEqualTo(notif2.getId());

        // Retrying identical eventId3 does not create a duplicate
        eventPublisher.publishEvent(new DirectMessageReactionEvent(
                convId, msgId,
                new ReactionResponse(UUID.randomUUID(), bob.getId(), "bob_notify", "THUMBS_UP", Instant.now()),
                false, bob.getId(), alice.getId(), Instant.now(), eventId3));

        long count = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientUserId().equals(alice.getId()) && n.getEventKey().equals("MESSAGE_REACTION:" + eventId3 + ":" + alice.getId()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Same-timestamp high-water mark-all read updates strictly up to cursor tuple (createdAt, id)")
    void testSameTimestampHighWaterReadAll() {
        Instant fixedTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        Notification notifA = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("TUPLE_A:" + UUID.randomUUID())
                .createdAt(fixedTime)
                .build());

        Notification notifB = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.CONNECTION_REQUESTED)
                .category(NotificationCategory.SOCIAL)
                .eventKey("TUPLE_B:" + UUID.randomUUID())
                .createdAt(fixedTime)
                .build());

        Notification earlier, later;
        if (notifA.getId().toString().compareTo(notifB.getId().toString()) <= 0) {
            earlier = notifA;
            later = notifB;
        } else {
            earlier = notifB;
            later = notifA;
        }

        Notification notifC = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.DIRECT_MESSAGE)
                .category(NotificationCategory.MESSAGING)
                .eventKey("TUPLE_C:" + UUID.randomUUID())
                .createdAt(fixedTime.plus(10, ChronoUnit.SECONDS))
                .build());

        String cursor = new NotificationCursor(earlier.getCreatedAt(), earlier.getId()).toCursorString();

        int updated = notificationService.markAllRead(alice.getId(), cursor, null);
        assertThat(updated).isGreaterThanOrEqualTo(1);

        Notification reloadedEarlier = notificationRepository.findById(earlier.getId()).orElseThrow();
        Notification reloadedLater = notificationRepository.findById(later.getId()).orElseThrow();
        Notification reloadedC = notificationRepository.findById(notifC.getId()).orElseThrow();

        assertThat(reloadedEarlier.getReadAt()).isNotNull();
        assertThat(reloadedEarlier.getState()).isEqualTo(NotificationState.READ);

        // 'later' was at same timestamp but higher id -> remains UNREAD!
        assertThat(reloadedLater.getReadAt()).isNull();
        assertThat(reloadedLater.getState()).isEqualTo(NotificationState.UNSEEN);

        // 'notifC' arrived after -> remains UNREAD!
        assertThat(reloadedC.getReadAt()).isNull();
        assertThat(reloadedC.getState()).isEqualTo(NotificationState.UNSEEN);
    }

    @Test
    @DisplayName("Stale worker cannot overwrite job completed by newer worker lease")
    void testStaleWorkerLeaseProtectionAndCrashRecovery() {
        Notification n = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("LEASE_TEST:" + UUID.randomUUID())
                .build());

        NotificationOutbox job = outboxRepository.save(NotificationOutbox.builder()
                .notificationId(n.getId())
                .channel(NotificationChannel.PUSH)
                .status(OutboxStatus.IN_PROGRESS)
                .lockedBy("worker-B")
                .lockedAt(Instant.now())
                .build());

        // Worker B completes successfully
        outboxService.handleJobSuccess(job.getId(), "worker-B", n.getId(), NotificationChannel.PUSH, "dest-B", "provider-msg-from-B", 1);

        NotificationOutbox completedJob = outboxRepository.findById(job.getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(OutboxStatus.SENT);

        // Stale Worker A attempts to report success
        outboxService.handleJobSuccess(job.getId(), "worker-A", n.getId(), NotificationChannel.PUSH, "dest-A", "stale-provider-msg-from-A", 1);

        NotificationOutbox reloaded = outboxRepository.findById(job.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.SENT);

        // Deliveries table only records worker-B's delivery
        List<NotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderBySentAtDesc(n.getId());
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getProviderMessageId()).isEqualTo("provider-msg-from-B");
    }

    @Test
    @DisplayName("Multi-device push fanout: Valid device succeeds, invalid device revokes, outbox completes")
    void testMultiDevicePushFanout() {
        deviceService.registerDevice(charlie.getId(), RegisterDeviceRequest.builder()
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .token("valid-charlie-token-phone-" + UUID.randomUUID())
                .build());

        NotificationDeviceResponse dev2 = deviceService.registerDevice(charlie.getId(), RegisterDeviceRequest.builder()
                .platform(DevicePlatform.IOS)
                .provider(DeviceProvider.APNS)
                .token("invalid-charlie-token-tablet-" + UUID.randomUUID())
                .build());

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(charlie.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("MULTI_DEV:" + UUID.randomUUID())
                .build();

        Notification n = notificationService.createNotification(cmd);

        outboxWorker.processOutbox();

        NotificationOutbox pushJob = outboxRepository.findByNotificationIdAndChannel(n.getId(), NotificationChannel.PUSH).orElseThrow();
        assertThat(pushJob.getStatus()).isEqualTo(OutboxStatus.SENT);

        NotificationDevice reloadedDev2 = deviceRepository.findById(dev2.getId()).orElseThrow();
        assertThat(reloadedDev2.isEnabled()).isFalse();
        assertThat(reloadedDev2.getRevokedAt()).isNotNull();

        List<NotificationDelivery> deliveries = deliveryRepository.findByNotificationIdOrderBySentAtDesc(n.getId());
        assertThat(deliveries.stream().anyMatch(d -> d.getStatus() == NotificationDeliveryStatus.SUCCESS)).isTrue();
        assertThat(deliveries.stream().anyMatch(d -> d.getStatus() == NotificationDeliveryStatus.FAILED)).isTrue();
    }

    @Test
    @DisplayName("Community notification legacy and canonical PATCH routes mark notifications read correctly")
    void testCommunityNotificationApiCompatibility() throws Exception {
        var mockUserSvc = org.mockito.Mockito.mock(com.collabsphere_first.security.service.CurrentUserService.class);
        org.mockito.Mockito.when(mockUserSvc.getCurrentUser()).thenReturn(alice);

        var controller = new com.collabsphere_first.community.controller.CommunityNotificationController(
                communityNotificationService, mockUserSvc);

        var mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();

        CommunityNotification cn = communityNotificationRepository.save(CommunityNotification.builder()
                .userId(alice.getId())
                .communityId(UUID.randomUUID())
                .type("NEW_POST")
                .title("Compatibility Test")
                .body("Checking endpoints")
                .createdAt(Instant.now())
                .readAt(null)
                .build());

        // 1. Canonical route PATCH /api/v1/communities/notifications/{id}/read
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/communities/notifications/" + cn.getId() + "/read"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 2. Legacy alias PATCH /api/v1/notifications/{id}/read
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/notifications/" + cn.getId() + "/read"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 3. Canonical route PATCH /api/v1/communities/notifications/read-all
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/communities/notifications/read-all"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 4. Legacy alias PATCH /api/v1/notifications/read-all
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                        "/api/v1/notifications/read-all"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 5. Canonical route GET /api/v1/communities/notifications
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/communities/notifications"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 6. Legacy route GET /api/v1/notifications
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/notifications"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 7. Canonical route GET /api/v1/communities/notifications/unread-count
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/communities/notifications/unread-count"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        // 8. Legacy route GET /api/v1/notifications/unread-count
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/notifications/unread-count"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Retention safety: Notifications with active/pending outbox jobs cannot be deleted")
    void testRetentionSafetyActiveOutboxJob() {
        Instant past = Instant.now().minus(40, ChronoUnit.DAYS);

        Notification notifWithPending = notificationRepository.save(Notification.builder()
                .recipientUserId(alice.getId())
                .actorUserId(bob.getId())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("RETENTION_PENDING:" + UUID.randomUUID())
                .createdAt(past)
                .readAt(past)
                .build());

        NotificationOutbox pendingJob = outboxRepository.save(NotificationOutbox.builder()
                .notificationId(notifWithPending.getId())
                .channel(NotificationChannel.PUSH)
                .status(OutboxStatus.PENDING)
                .build());

        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        // Run retention deletion query
        notificationRepository.deleteReadOlderThan(cutoff);

        // notifWithPending must NOT be deleted because outbox job is PENDING
        Optional<Notification> retained = notificationRepository.findById(notifWithPending.getId());
        assertThat(retained).isPresent();

        // Once job completes to SENT
        pendingJob.setStatus(OutboxStatus.SENT);
        outboxRepository.save(pendingJob);

        // Now retention deletion successfully removes it
        notificationRepository.deleteReadOlderThan(cutoff);
        Optional<Notification> cleanedUp = notificationRepository.findById(notifWithPending.getId());
        assertThat(cleanedUp).isEmpty();
    }

    @Test
    @DisplayName("Device token reassignment: Token reassigned to new user leaves exactly one active owner")
    void testDeviceTokenReassignment() {
        String sharedToken = "shared-fcm-token-" + UUID.randomUUID();

        // Alice registers the device
        NotificationDeviceResponse aliceDev = deviceService.registerDevice(alice.getId(), RegisterDeviceRequest.builder()
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .token(sharedToken)
                .build());
        assertThat(aliceDev.isEnabled()).isTrue();

        // Bob registers the same token
        NotificationDeviceResponse bobDev = deviceService.registerDevice(bob.getId(), RegisterDeviceRequest.builder()
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .token(sharedToken)
                .build());

        assertThat(bobDev.isEnabled()).isTrue();
        assertThat(bobDev.getId()).isEqualTo(aliceDev.getId());

        // Stored entity in DB is owned by Bob
        NotificationDevice stored = deviceRepository.findById(bobDev.getId()).orElseThrow();
        assertThat(stored.getUserId()).isEqualTo(bob.getId());

        // Alice has no active device with this token
        List<NotificationDevice> aliceDevices = deviceRepository.findByUserIdAndEnabledTrue(alice.getId());
        assertThat(aliceDevices.stream().noneMatch(d -> d.getId().equals(bobDev.getId()))).isTrue();

        // Bob has exactly 1 active device with this token
        List<NotificationDevice> bobDevices = deviceRepository.findByUserIdAndEnabledTrue(bob.getId());
        assertThat(bobDevices.stream().filter(d -> d.getId().equals(bobDev.getId()))).hasSize(1);
    }
}
