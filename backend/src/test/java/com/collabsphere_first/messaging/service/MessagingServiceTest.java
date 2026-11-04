package com.collabsphere_first.messaging.service;

import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.cursor.MessagingCursorCodec;
import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.mapper.MessagingMapper;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.repository.*;
import com.collabsphere_first.messaging.security.MessagingAccessService;
import com.collabsphere_first.messaging.security.MessagingRateLimiter;
import com.collabsphere_first.messaging.websocket.MessagingRealtimePublisher;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private MessageAttachmentRepository attachmentRepository;
    @Mock private MessageReactionRepository reactionRepository;
    @Mock private MessageReceiptRepository receiptRepository;
    @Mock private UserRepository userRepository;

    @Mock private MessagingAccessService accessService;
    @Mock private MessagingRealtimePublisher realtimePublisher;
    @Mock private MessagingAttachmentService attachmentService;
    @Mock private MessagingRateLimiter rateLimiter;
    @Mock private MessagingPresenceService presenceService;
    @Mock private MessagingCursorCodec cursorCodec;
    @Mock private MessagingProperties properties;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private IdempotentMessagePersister persister;
    @Mock private org.springframework.beans.factory.ObjectProvider<com.collabsphere_first.integration.storage.StorageService> storageServiceProvider;

    private MessagingMapper mapper;
    private MessagingServiceImpl messagingService;

    private final Long userA = 10L;
    private final Long userB = 20L;
    private UUID conversationId;
    private User uA;
    private User uB;

    @BeforeEach
    void setUp() {
        mapper = new MessagingMapper(attachmentService);
        messagingService = new MessagingServiceImpl(
                conversationRepository,
                participantRepository,
                messageRepository,
                attachmentRepository,
                reactionRepository,
                receiptRepository,
                userRepository,
                accessService,
                realtimePublisher,
                attachmentService,
                rateLimiter,
                presenceService,
                cursorCodec,
                mapper,
                properties,
                eventPublisher,
                persister,
                storageServiceProvider
        );

        conversationId = UUID.randomUUID();
        uA = User.builder().id(userA).username("alice").email("alice@example.com").build();
        uB = User.builder().id(userB).username("bob").email("bob@example.com").build();

        lenient().when(properties.getMaxMessageBodyLength()).thenReturn(10000);
        lenient().when(properties.getDefaultPageSize()).thenReturn(20);
        lenient().when(properties.getMaxPageSize()).thenReturn(50);
        lenient().when(properties.getMessageEditWindow()).thenReturn(java.time.Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("getOrCreateDirectConversation: creates new normalized conversation if not existing")
    void getOrCreateDirectConversation_createsNew() {
        when(conversationRepository.findByDirectUserLowIdAndDirectUserHighId(10L, 20L)).thenReturn(Optional.empty());

        Conversation savedConv = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .directUserLowId(10L)
                .directUserHighId(20L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(conversationRepository.saveAndFlush(any(Conversation.class))).thenReturn(savedConv);
        when(userRepository.getReferenceById(10L)).thenReturn(uA);
        when(userRepository.getReferenceById(20L)).thenReturn(uB);
        when(userRepository.findById(20L)).thenReturn(Optional.of(uB));

        ConversationResponse response = messagingService.getOrCreateDirectConversation(userA, userB);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(conversationId);
        assertThat(response.type()).isEqualTo(ConversationType.DIRECT);
        assertThat(response.otherParticipant().id()).isEqualTo(userB);
        verify(participantRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("getOrCreateDirectConversation: reverse pair B->A returns existing conversation")
    void getOrCreateDirectConversation_reversePairReturnsExisting() {
        Conversation existing = Conversation.builder()
                .id(conversationId)
                .type(ConversationType.DIRECT)
                .directUserLowId(10L)
                .directUserHighId(20L)
                .build();
        when(conversationRepository.findByDirectUserLowIdAndDirectUserHighId(10L, 20L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(10L)).thenReturn(Optional.of(uA));

        ConversationResponse response = messagingService.getOrCreateDirectConversation(userB, userA);

        assertThat(response.id()).isEqualTo(conversationId);
        assertThat(response.otherParticipant().id()).isEqualTo(userA);
        verify(conversationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("sendMessage: sends valid text message and updates conversation lastMessage")
    void sendMessage_success() {
        when(accessService.getOtherParticipantId(conversationId, userA)).thenReturn(userB);
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));
        when(userRepository.findById(userB)).thenReturn(Optional.of(uB));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(uA, uB));

        UUID clientMsgId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest(clientMsgId, "Hello Bob!", null, null);

        when(persister.persistMessage(eq(conversationId), eq(userA), eq(userB), eq(request), any(), eq(true)))
                .thenAnswer(invocation -> {
                    DirectMessage m = DirectMessage.builder()
                            .id(UUID.randomUUID())
                            .conversationId(conversationId)
                            .senderId(userA)
                            .clientMessageId(clientMsgId)
                            .messageType(MessageType.TEXT)
                            .body("Hello Bob!")
                            .createdAt(Instant.now())
                            .build();
                    return new PersistMessageResult(m, true);
                });

        MessageResponse response = messagingService.sendMessage(conversationId, userA, request);

        assertThat(response).isNotNull();
        assertThat(response.body()).isEqualTo("Hello Bob!");
        assertThat(response.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(response.clientMessageId()).isEqualTo(clientMsgId);

        verify(realtimePublisher).publishMessageCreated(any(MessageResponse.class), eq("bob@example.com"), eq("alice@example.com"));
    }

    @Test
    @DisplayName("sendMessage: duplicate clientMessageId returns existing message idempotently")
    void sendMessage_idempotent() {
        UUID clientMsgId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest(clientMsgId, "Duplicate tap", null, null);

        DirectMessage existing = DirectMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(userA)
                .clientMessageId(clientMsgId)
                .messageType(MessageType.TEXT)
                .body("Duplicate tap")
                .status(MessageStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(messageRepository.findByConversationIdAndSenderIdAndClientMessageId(conversationId, userA, clientMsgId))
                .thenReturn(Optional.of(existing));
        when(attachmentRepository.findByMessageIdOrderByDisplayOrderAsc(existing.getId())).thenReturn(List.of());
        when(userRepository.findAllById(anySet())).thenReturn(List.of(uA));

        MessageResponse response = messagingService.sendMessage(conversationId, userA, request);

        assertThat(response.id()).isEqualTo(existing.getId());
        assertThat(response.body()).isEqualTo("Duplicate tap");
        verify(persister, never()).persistMessage(any(), any(), any(), any(), any(), anyBoolean());
        verify(realtimePublisher, never()).publishMessageCreated(any(), any(), any());
    }

    @Test
    @DisplayName("sendMessage: empty body and no attachments is rejected")
    void sendMessage_emptyRejected() {
        SendMessageRequest request = new SendMessageRequest(null, "   ", null, null);

        assertThatThrownBy(() -> messagingService.sendMessage(conversationId, userA, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message must contain either a non-blank body or at least one attachment");
    }

    @Test
    @DisplayName("sendMessage: cross-conversation reply is rejected")
    void sendMessage_crossConversationReplyRejected() {
        UUID replyToId = UUID.randomUUID();
        UUID otherConvId = UUID.randomUUID();
        DirectMessage otherMsg = DirectMessage.builder().id(replyToId).conversationId(otherConvId).build();

        when(messageRepository.findById(replyToId)).thenReturn(Optional.of(otherMsg));

        SendMessageRequest request = new SendMessageRequest(null, "Reply across rooms", replyToId, null);

        assertThatThrownBy(() -> messagingService.sendMessage(conversationId, userA, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reply to a message from a different conversation");
    }

    @Test
    @DisplayName("editMessage: sender can edit active message")
    void editMessage_senderSucceeds() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder()
                .id(msgId)
                .conversationId(conversationId)
                .senderId(userA)
                .body("Original")
                .status(MessageStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageRepository.save(any(DirectMessage.class))).thenReturn(msg);
        when(accessService.getOtherParticipantId(conversationId, userA)).thenReturn(userB);
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));
        when(userRepository.findById(userB)).thenReturn(Optional.of(uB));
        when(userRepository.findAllById(anySet())).thenReturn(List.of(uA, uB));

        EditMessageRequest request = new EditMessageRequest("Edited content");
        MessageResponse response = messagingService.editMessage(msgId, userA, request);

        assertThat(response.body()).isEqualTo("Edited content");
        assertThat(msg.getEditedAt()).isNotNull();
        verify(realtimePublisher).publishMessageUpdated(any(), eq("bob@example.com"), eq("alice@example.com"));
    }

    @Test
    @DisplayName("editMessage: non-sender cannot edit other user's message")
    void editMessage_nonSenderRejected() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder()
                .id(msgId)
                .conversationId(conversationId)
                .senderId(userB)
                .body("Bob's message")
                .status(MessageStatus.ACTIVE)
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        EditMessageRequest request = new EditMessageRequest("Alice trying to tamper");

        assertThatThrownBy(() -> messagingService.editMessage(msgId, userA, request))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("Only the original sender can edit");
    }

    @Test
    @DisplayName("deleteMessage: sender soft-deletes message and publishes event")
    void deleteMessage_softDeletes() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder()
                .id(msgId)
                .conversationId(conversationId)
                .senderId(userA)
                .body("To be deleted")
                .status(MessageStatus.ACTIVE)
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(accessService.getOtherParticipantId(conversationId, userA)).thenReturn(userB);
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));
        when(userRepository.findById(userB)).thenReturn(Optional.of(uB));

        messagingService.deleteMessage(msgId, userA);

        assertThat(msg.getStatus()).isEqualTo(MessageStatus.DELETED);
        assertThat(msg.getDeletedAt()).isNotNull();
        verify(messageRepository).save(msg);
        verify(realtimePublisher).publishMessageDeleted(conversationId, msgId, "bob@example.com", "alice@example.com");
    }

    @Test
    @DisplayName("addOrUpdateReaction: participant adds reaction to active message")
    void addOrUpdateReaction_success() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder()
                .id(msgId)
                .conversationId(conversationId)
                .senderId(userB)
                .status(MessageStatus.ACTIVE)
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(reactionRepository.findByMessageIdAndUserId(msgId, userA)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(MessageReaction.class))).thenAnswer(i -> {
            MessageReaction r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));
        when(accessService.getOtherParticipantId(conversationId, userA)).thenReturn(userB);
        when(userRepository.findById(userB)).thenReturn(Optional.of(uB));

        ReactionRequest request = new ReactionRequest("👍");
        ReactionResponse response = messagingService.addOrUpdateReaction(msgId, userA, request);

        assertThat(response).isNotNull();
        assertThat(response.reaction()).isEqualTo("👍");
        verify(realtimePublisher).publishReactionUpdated(eq(conversationId), eq(msgId), any(), eq(false), eq("bob@example.com"), eq("alice@example.com"));
    }

    @Test
    @DisplayName("addOrUpdateReaction: reaction on deleted message is rejected")
    void addOrUpdateReaction_deletedRejected() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder()
                .id(msgId)
                .conversationId(conversationId)
                .status(MessageStatus.DELETED)
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

        ReactionRequest request = new ReactionRequest("❤️");
        assertThatThrownBy(() -> messagingService.addOrUpdateReaction(msgId, userA, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot react to a deleted message");
    }

    @Test
    @DisplayName("markDelivered: sets deliveredAt idempotently")
    void markDelivered_idempotent() {
        UUID msgId = UUID.randomUUID();
        DirectMessage msg = DirectMessage.builder().id(msgId).conversationId(conversationId).senderId(userA).build();
        MessageReceipt receipt = MessageReceipt.builder()
                .id(new MessageReceiptId(msgId, userB))
                .deliveredAt(null)
                .build();

        when(receiptRepository.findByIdMessageIdAndIdUserId(msgId, userB)).thenReturn(Optional.of(receipt));
        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));

        messagingService.markDelivered(msgId, userB);

        assertThat(receipt.getDeliveredAt()).isNotNull();
        verify(receiptRepository).save(receipt);
        verify(realtimePublisher).publishDeliveryReceipt(eq(conversationId), eq(msgId), any(), eq("alice@example.com"));

        // Second call when already delivered does nothing
        messagingService.markDelivered(msgId, userB);
        verify(receiptRepository, times(1)).save(receipt);
    }

    @Test
    @DisplayName("markConversationRead: advances cursor monotonically and marks receipts")
    void markConversationRead_monotonic() {
        UUID msgId = UUID.randomUUID();
        Instant msgTime = Instant.now();
        DirectMessage msg = DirectMessage.builder().id(msgId).conversationId(conversationId).createdAt(msgTime).build();

        ConversationParticipant participant = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, userB))
                .lastReadMessageId(null)
                .lastReadAt(msgTime.minusSeconds(10))
                .build();

        when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
        when(participantRepository.findByIdConversationIdAndIdUserId(conversationId, userB)).thenReturn(Optional.of(participant));
        when(accessService.getOtherParticipantId(conversationId, userB)).thenReturn(userA);
        when(userRepository.findById(userA)).thenReturn(Optional.of(uA));

        ReadReceiptRequest request = new ReadReceiptRequest(msgId);
        messagingService.markConversationRead(conversationId, userB, request);

        assertThat(participant.getLastReadMessageId()).isEqualTo(msgId);
        assertThat(participant.getLastReadAt()).isNotNull();
        verify(receiptRepository).markReadForUserUpTo(eq(conversationId), eq(userB), eq(msgTime), eq(msgId), any());
        verify(realtimePublisher).publishReadReceipt(eq(conversationId), eq(msgId), any(), eq("alice@example.com"));

        // Calling with an older message does not move cursor backward
        DirectMessage olderMsg = DirectMessage.builder().id(UUID.randomUUID()).conversationId(conversationId).createdAt(msgTime.minusSeconds(20)).build();
        when(messageRepository.findById(olderMsg.getId())).thenReturn(Optional.of(olderMsg));
        messagingService.markConversationRead(conversationId, userB, new ReadReceiptRequest(olderMsg.getId()));

        assertThat(participant.getLastReadMessageId()).isEqualTo(msgId); // unchanged
    }

    @Test
    @DisplayName("muteConversation: updates participant notifications_muted")
    void muteConversation_success() {
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, userA))
                .notificationsMuted(false)
                .build();
        when(participantRepository.findByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(Optional.of(participant));

        messagingService.muteConversation(conversationId, userA, true);

        assertThat(participant.isNotificationsMuted()).isTrue();
        verify(participantRepository).save(participant);
    }

    @Test
    @DisplayName("archiveConversation: updates participant archived state")
    void archiveConversation_success() {
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, userA))
                .archived(false)
                .build();
        when(participantRepository.findByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(Optional.of(participant));

        messagingService.archiveConversation(conversationId, userA, true);

        assertThat(participant.isArchived()).isTrue();
        verify(participantRepository).save(participant);
    }
}
