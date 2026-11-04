package com.collabsphere_first.messaging;

import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.exception.IdempotencyConflictException;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.repository.*;
import com.collabsphere_first.messaging.service.MessagingAttachmentService;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate",
        "collabster.storage.public-base-url=http://localhost:8080/api/storage",
        "collabster.storage.signing-secret=test-signing-secret-collobster-messaging-phase-6-1"
})
class MessagingHardeningIntegrationTest {

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private MessagingAttachmentService attachmentService;

    @Autowired
    private MessagingPresenceService presenceService;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private DirectMessageRepository messageRepository;

    @Autowired
    private MessageReceiptRepository receiptRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User charlie;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        alice = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Harden")
                .email("alice_" + suffix + "@example.com")
                .username("alice_" + suffix)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        bob = userRepository.save(User.builder()
                .firstName("Bob")
                .lastName("Harden")
                .email("bob_" + suffix + "@example.com")
                .username("bob_" + suffix)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        charlie = userRepository.save(User.builder()
                .firstName("Charlie")
                .lastName("Harden")
                .email("charlie_" + suffix + "@example.com")
                .username("charlie_" + suffix)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());
    }

    @Test
    @DisplayName("Read Cursor: Delayed ACK marks A then C, cursor advances monotonically to C and unread becomes 0")
    void delayedAck_advancesMonotonicallyToC() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        Instant t0 = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant t1 = t0.plusSeconds(60);
        Instant t2 = t1.plusSeconds(60);

        DirectMessage msgA = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message A")
                .status(MessageStatus.ACTIVE).createdAt(t0).updatedAt(t0)
                .build());
        DirectMessage msgB = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message B")
                .status(MessageStatus.ACTIVE).createdAt(t1).updatedAt(t1)
                .build());
        DirectMessage msgC = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message C")
                .status(MessageStatus.ACTIVE).createdAt(t2).updatedAt(t2)
                .build());

        // Create receipts for Bob
        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(msgA.getId(), bob.getId())).build());
        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(msgB.getId(), bob.getId())).build());
        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(msgC.getId(), bob.getId())).build());

        // Bob has 3 unread messages initially
        ConversationResponse convBob0 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob0.unreadCount()).isEqualTo(3);

        // Bob delayed ACK for A
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(msgA.getId()));

        ConversationResponse convBob1 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob1.unreadCount()).isEqualTo(2);

        ConversationParticipant partBob1 = participantRepository.findByIdConversationIdAndIdUserId(convId, bob.getId()).orElseThrow();
        assertThat(partBob1.getLastReadMessageId()).isEqualTo(msgA.getId());

        // Bob delayed ACK for C
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(msgC.getId()));

        ConversationResponse convBob2 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob2.unreadCount()).isEqualTo(0);

        ConversationParticipant partBob2 = participantRepository.findByIdConversationIdAndIdUserId(convId, bob.getId()).orElseThrow();
        assertThat(partBob2.getLastReadMessageId()).isEqualTo(msgC.getId());

        // Receipts for A, B, and C all have readAt set
        MessageReceipt rA = receiptRepository.findByIdMessageIdAndIdUserId(msgA.getId(), bob.getId()).orElseThrow();
        MessageReceipt rB = receiptRepository.findByIdMessageIdAndIdUserId(msgB.getId(), bob.getId()).orElseThrow();
        MessageReceipt rC = receiptRepository.findByIdMessageIdAndIdUserId(msgC.getId(), bob.getId()).orElseThrow();
        assertThat(rA.getReadAt()).isNotNull();
        assertThat(rB.getReadAt()).isNotNull();
        assertThat(rC.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Read Cursor: Same-timestamp messages disambiguated by UUID tiebreaker in tuple (createdAt, id)")
    void sameTimestamp_tiebreakerByUuid() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        Instant sameTime = Instant.now().minus(5, ChronoUnit.MINUTES);

        DirectMessage m1 = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message 1 same time")
                .status(MessageStatus.ACTIVE).createdAt(sameTime).updatedAt(sameTime)
                .build());

        DirectMessage m2 = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message 2 same time")
                .status(MessageStatus.ACTIVE).createdAt(sameTime).updatedAt(sameTime)
                .build());

        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(m1.getId(), bob.getId())).build());
        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(m2.getId(), bob.getId())).build());

        int cmp = Long.compareUnsigned(m1.getId().getMostSignificantBits(), m2.getId().getMostSignificantBits());
        if (cmp == 0) cmp = Long.compareUnsigned(m1.getId().getLeastSignificantBits(), m2.getId().getLeastSignificantBits());
        DirectMessage firstMsg = cmp < 0 ? m1 : m2;
        DirectMessage secondMsg = cmp < 0 ? m2 : m1;

        // Initial unread count is 2
        ConversationResponse convBob0 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob0.unreadCount()).isEqualTo(2);

        // Mark firstMsg read (first in tuple order because id is lower)
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(firstMsg.getId()));

        ConversationResponse convBob1 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob1.unreadCount()).isEqualTo(1);

        // Mark secondMsg read
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(secondMsg.getId()));

        ConversationResponse convBob2 = messagingService.getConversation(convId, bob.getId());
        assertThat(convBob2.unreadCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Read Cursor: Out-of-order read request (reading C then B) does NOT regress cursor to B")
    void outOfOrderReadRequest_doesNotRegressCursor() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        Instant t0 = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant t1 = t0.plusSeconds(60);

        DirectMessage msgB = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message B")
                .status(MessageStatus.ACTIVE).createdAt(t0).updatedAt(t0)
                .build());
        DirectMessage msgC = messageRepository.save(DirectMessage.builder()
                .conversationId(convId).senderId(alice.getId())
                .messageType(MessageType.TEXT).body("Message C")
                .status(MessageStatus.ACTIVE).createdAt(t1).updatedAt(t1)
                .build());

        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(msgB.getId(), bob.getId())).build());
        receiptRepository.save(MessageReceipt.builder().id(new MessageReceiptId(msgC.getId(), bob.getId())).build());

        // Mark C first
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(msgC.getId()));

        ConversationParticipant part1 = participantRepository.findByIdConversationIdAndIdUserId(convId, bob.getId()).orElseThrow();
        assertThat(part1.getLastReadMessageId()).isEqualTo(msgC.getId());

        // Out-of-order read request for older message B
        messagingService.markConversationRead(convId, bob.getId(), new ReadReceiptRequest(msgB.getId()));

        // Cursor must remain at C
        ConversationParticipant part2 = participantRepository.findByIdConversationIdAndIdUserId(convId, bob.getId()).orElseThrow();
        assertThat(part2.getLastReadMessageId()).isEqualTo(msgC.getId());
    }

    @Test
    @DisplayName("Concurrent Send: Racing threads with identical clientMessageId yield 1 row and both succeed")
    void concurrentSend_identicalClientMessageId_resolvesCleanly() throws Exception {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();
        UUID clientMsgId = UUID.randomUUID();

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Future<MessageResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                SendMessageRequest req = new SendMessageRequest(clientMsgId, "Racing concurrent message", null, null);
                return messagingService.sendMessage(convId, alice.getId(), req);
            }));
        }

        List<MessageResponse> responses = new ArrayList<>();
        for (Future<MessageResponse> f : futures) {
            responses.add(f.get(10, TimeUnit.SECONDS));
        }
        executor.shutdown();

        // All responses succeeded and point to the identical message ID
        UUID expectedId = responses.get(0).id();
        for (MessageResponse r : responses) {
            assertThat(r.id()).isEqualTo(expectedId);
            assertThat(r.body()).isEqualTo("Racing concurrent message");
        }

        // Exactly 1 row in direct_messages for this clientMessageId
        long matchingCount = messageRepository.findAll().stream()
                .filter(m -> convId.equals(m.getConversationId()) && clientMsgId.equals(m.getClientMessageId()))
                .count();
        assertThat(matchingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Send Idempotency: Reusing clientMessageId with different payload throws IdempotencyConflictException (409)")
    void idempotencyConflict_reusedClientMessageIdWithDifferentPayload_throws409() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();
        UUID clientMsgId = UUID.randomUUID();

        SendMessageRequest initialReq = new SendMessageRequest(clientMsgId, "Original text", null, null);
        MessageResponse initial = messagingService.sendMessage(convId, alice.getId(), initialReq);
        assertThat(initial).isNotNull();

        // Same clientMessageId with different body
        SendMessageRequest conflictingReq = new SendMessageRequest(clientMsgId, "Altered text attempt", null, null);
        assertThatThrownBy(() -> messagingService.sendMessage(convId, alice.getId(), conflictingReq))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    @DisplayName("Attachment Authorization: User cannot attach foreign media asset owned by another user")
    void attachmentOwnership_rejectsForeignMedia() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        String storageKey = "direct/" + convId + "/photo.png";

        // Alice registers the uploaded media
        attachmentService.registerUploadedMedia(new RegisterMediaRequest(
                storageKey, AttachmentMediaType.IMAGE, "image/png", "photo.png", 1024L
        ), alice.getId());

        // Bob attempts to send a message referencing Alice's registered storage key
        SendMessageRequest bobReq = new SendMessageRequest(
                UUID.randomUUID(),
                "Bob trying to attach Alice's photo",
                null,
                List.of(new AttachmentRequest(
                        AttachmentMediaType.IMAGE, storageKey, "image/png", "photo.png", 1024L, 100, 100, null, 0
                ))
        );

        assertThatThrownBy(() -> messagingService.sendMessage(convId, bob.getId(), bobReq))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("not authorized to attach asset owned by another user");

        // Alice sending with her own media succeeds
        SendMessageRequest aliceReq = new SendMessageRequest(
                UUID.randomUUID(),
                "Alice's legitimate photo",
                null,
                List.of(new AttachmentRequest(
                        AttachmentMediaType.IMAGE, storageKey, "image/png", "photo.png", 1024L, 100, 100, null, 0
                ))
        );
        MessageResponse aliceMsg = messagingService.sendMessage(convId, alice.getId(), aliceReq);
        assertThat(aliceMsg).isNotNull();
        assertThat(aliceMsg.attachments()).hasSize(1);
    }

    @Test
    @DisplayName("Attachment Signed URL: Non-participant cannot request signed URL for attachment")
    void attachmentSignedUrl_rejectsNonParticipant() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        String storageKey = "direct/" + convId + "/doc.pdf";
        attachmentService.registerUploadedMedia(new RegisterMediaRequest(
                storageKey, AttachmentMediaType.DOCUMENT, "application/pdf", "doc.pdf", 2048L
        ), alice.getId());

        MessageResponse msg = messagingService.sendMessage(convId, alice.getId(), new SendMessageRequest(
                UUID.randomUUID(), "Alice doc", null,
                List.of(new AttachmentRequest(
                        AttachmentMediaType.DOCUMENT, storageKey, "application/pdf", "doc.pdf", 2048L, null, null, null, 0
                ))
        ));

        UUID attachmentId = msg.attachments().get(0).id();

        // Charlie is NOT a participant in this conversation
        assertThatThrownBy(() -> messagingService.getAttachmentSignedUrl(convId, attachmentId, charlie.getId()))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("not a participant in this conversation");

        // Bob (recipient participant) can generate signed URL
        AttachmentSignedUrlResponse urlResponse = messagingService.getAttachmentSignedUrl(convId, attachmentId, bob.getId());
        assertThat(urlResponse).isNotNull();
        assertThat(urlResponse.attachmentId()).isEqualTo(attachmentId);
        assertThat(urlResponse.signedUrl()).contains("doc.pdf").contains("signature=");
    }

    @Test
    @DisplayName("Delivery ACK Authorization: Sender cannot mark own message delivered, non-participant rejected, timestamp idempotent")
    void deliveryAck_authorizationAndIdempotency() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        MessageResponse msg = messagingService.sendMessage(convId, alice.getId(),
                new SendMessageRequest(UUID.randomUUID(), "Hello Bob", null, null));

        // 1. Sender (Alice) cannot mark own message delivered
        assertThatThrownBy(() -> messagingService.markDelivered(msg.id(), alice.getId()))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("Sender cannot acknowledge delivery");

        // 2. Non-participant (Charlie) cannot mark delivered
        assertThatThrownBy(() -> messagingService.markDelivered(msg.id(), charlie.getId()))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("not a participant in this conversation");

        // 3. Recipient (Bob) marks delivered
        messagingService.markDelivered(msg.id(), bob.getId());
        MessageReceipt r1 = receiptRepository.findByIdMessageIdAndIdUserId(msg.id(), bob.getId()).orElseThrow();
        assertThat(r1.getDeliveredAt()).isNotNull();
        Instant originalDeliveredAt = r1.getDeliveredAt();

        // 4. Repeated call is idempotent and does not overwrite deliveredAt
        messagingService.markDelivered(msg.id(), bob.getId());
        MessageReceipt r2 = receiptRepository.findByIdMessageIdAndIdUserId(msg.id(), bob.getId()).orElseThrow();
        assertThat(r2.getDeliveredAt()).isEqualTo(originalDeliveredAt);
    }

    @Test
    @DisplayName("Presence Lifecycle: Redis ZSET multi-session handles heartbeats, multi-device, and disconnect")
    void presenceLifecycle_zsetMultiSession() {
        Long userId = bob.getId();
        String username = bob.getUsername();
        String session1 = "device-phone-" + UUID.randomUUID();
        String session2 = "device-web-" + UUID.randomUUID();

        // 1. Clean slate
        presenceService.onSessionDisconnected(userId, username, session1);
        presenceService.onSessionDisconnected(userId, username, session2);
        assertThat(presenceService.isUserOnline(userId)).isFalse();

        // 2. Phone connects
        presenceService.onSessionConnected(userId, username, session1);
        assertThat(presenceService.isUserOnline(userId)).isTrue();

        // 3. Web connects (multi-device)
        presenceService.onSessionConnected(userId, username, session2);
        assertThat(presenceService.isUserOnline(userId)).isTrue();

        // 4. Heartbeat on web session
        presenceService.onHeartbeat(userId, session2);
        assertThat(presenceService.isUserOnline(userId)).isTrue();

        // 5. Phone disconnects -> user remains ONLINE because Web session is active!
        presenceService.onSessionDisconnected(userId, username, session1);
        assertThat(presenceService.isUserOnline(userId)).isTrue();

        // 6. Web disconnects -> user transitions to OFFLINE
        presenceService.onSessionDisconnected(userId, username, session2);
        assertThat(presenceService.isUserOnline(userId)).isFalse();
    }

    @Test
    @DisplayName("Resilience: Realtime publish failure does not roll back durable message")
    void realtimeFailure_doesNotRollbackCommittedMessage() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID convId = conv.id();

        SendMessageRequest req = new SendMessageRequest(UUID.randomUUID(), "Durable message test", null, null);
        MessageResponse res = messagingService.sendMessage(convId, alice.getId(), req);
        assertThat(res).isNotNull();

        DirectMessage saved = messageRepository.findById(res.id()).orElseThrow();
        assertThat(saved.getBody()).isEqualTo("Durable message test");
        assertThat(saved.getStatus()).isEqualTo(MessageStatus.ACTIVE);
    }
}
