package com.collabsphere_first.messaging;

import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.Conversation;
import com.collabsphere_first.messaging.entity.DirectMessage;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.exception.BlockedUserException;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.presence.MessagingTypingService;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.messaging.repository.DirectMessageRepository;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.network.entity.UserBlock;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Transactional
class MessagingPostgresIntegrationTest {

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DirectMessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private MessagingTypingService typingService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        alice = userRepository.save(User.builder()
                .firstName("Alice")
                .lastName("Tester")
                .email("alice_" + suffix + "@example.com")
                .username("alice_" + suffix)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());

        bob = userRepository.save(User.builder()
                .firstName("Bob")
                .lastName("Tester")
                .email("bob_" + suffix + "@example.com")
                .username("bob_" + suffix)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build());
    }

    @Test
    @DisplayName("DB Safety: Database check constraint rejects direct_user_low_id >= direct_user_high_id")
    void dbCheckConstraint_rejectsUnnormalizedPair() {
        Conversation invalid = Conversation.builder()
                .type(ConversationType.DIRECT)
                .directUserLowId(bob.getId() > alice.getId() ? bob.getId() : alice.getId() + 10)
                .directUserHighId(bob.getId() > alice.getId() ? alice.getId() : alice.getId())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> conversationRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Safety: Database unique constraint rejects duplicate direct conversation pairs")
    void dbUniqueConstraint_rejectsDuplicatePairs() {
        long low = Math.min(alice.getId(), bob.getId());
        long high = Math.max(alice.getId(), bob.getId());

        Conversation c1 = Conversation.builder()
                .type(ConversationType.DIRECT)
                .directUserLowId(low)
                .directUserHighId(high)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        conversationRepository.saveAndFlush(c1);

        Conversation c2 = Conversation.builder()
                .type(ConversationType.DIRECT)
                .directUserLowId(low)
                .directUserHighId(high)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> conversationRepository.saveAndFlush(c2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("DB Safety: Unique index rejects duplicate clientMessageId per conversation and sender")
    void dbUniqueConstraint_rejectsDuplicateClientMessageId() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        UUID clientMsgId = UUID.randomUUID();

        DirectMessage m1 = DirectMessage.builder()
                .conversationId(conv.id())
                .senderId(alice.getId())
                .clientMessageId(clientMsgId)
                .messageType(MessageType.TEXT)
                .body("First send")
                .status(MessageStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        messageRepository.saveAndFlush(m1);

        DirectMessage m2 = DirectMessage.builder()
                .conversationId(conv.id())
                .senderId(alice.getId())
                .clientMessageId(clientMsgId)
                .messageType(MessageType.TEXT)
                .body("Duplicate send attempt")
                .status(MessageStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        assertThatThrownBy(() -> messageRepository.saveAndFlush(m2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Full Flow: Create conversation, send message, edit, reaction, read receipt")
    void fullConversationLifecycle() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());
        assertThat(conv).isNotNull();
        assertThat(conv.otherParticipant().id()).isEqualTo(bob.getId());

        // Alice sends message
        SendMessageRequest sendReq = new SendMessageRequest(UUID.randomUUID(), "Hello Bob from integration test", null, null);
        MessageResponse sent = messagingService.sendMessage(conv.id(), alice.getId(), sendReq);
        assertThat(sent.body()).isEqualTo("Hello Bob from integration test");
        assertThat(sent.deleted()).isFalse();

        // Bob reads conversation
        MessagePageResponse bobMessages = messagingService.getMessages(conv.id(), bob.getId(), null, 10);
        assertThat(bobMessages.items()).hasSize(1);
        assertThat(bobMessages.items().get(0).id()).isEqualTo(sent.id());

        // Bob marks as read
        messagingService.markConversationRead(conv.id(), bob.getId(), new ReadReceiptRequest(sent.id()));

        // Alice views conversations: unread count is 0
        ConversationPageResponse aliceConvs = messagingService.getConversations(alice.getId(), null, 10);
        assertThat(aliceConvs.items()).hasSize(1);
        assertThat(aliceConvs.items().get(0).unreadCount()).isEqualTo(0);

        // Bob reacts to Alice's message
        ReactionResponse reaction = messagingService.addOrUpdateReaction(sent.id(), bob.getId(), new ReactionRequest("🚀"));
        assertThat(reaction.reaction()).isEqualTo("🚀");

        // Alice edits message
        MessageResponse edited = messagingService.editMessage(sent.id(), alice.getId(), new EditMessageRequest("Hello Bob (Edited)"));
        assertThat(edited.body()).isEqualTo("Hello Bob (Edited)");
        assertThat(edited.editedAt()).isNotNull();

        // Alice deletes message
        messagingService.deleteMessage(sent.id(), alice.getId());

        MessagePageResponse afterDelete = messagingService.getMessages(conv.id(), bob.getId(), null, 10);
        assertThat(afterDelete.items().get(0).deleted()).isTrue();
        assertThat(afterDelete.items().get(0).body()).isNull();
    }

    @Test
    @DisplayName("Block Semantics: Block freezes conversation to read-only; unblock restores eligibility")
    void blockSemantics_freezesConversation() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());

        // Alice sends an initial message
        MessageResponse m1 = messagingService.sendMessage(conv.id(), alice.getId(),
                new SendMessageRequest(UUID.randomUUID(), "Before block message", null, null));
        assertThat(m1).isNotNull();

        // Bob blocks Alice
        UserBlock block = UserBlock.builder()
                .blocker(bob)
                .blocked(alice)
                .createdAt(Instant.now())
                .build();
        userBlockRepository.saveAndFlush(block);

        // 1. Existing history remains readable by both Alice and Bob
        MessagePageResponse aliceView = messagingService.getMessages(conv.id(), alice.getId(), null, 10);
        assertThat(aliceView.items()).hasSize(1);
        MessagePageResponse bobView = messagingService.getMessages(conv.id(), bob.getId(), null, 10);
        assertThat(bobView.items()).hasSize(1);

        // 2. Alice cannot send new messages
        assertThatThrownBy(() -> messagingService.sendMessage(conv.id(), alice.getId(),
                new SendMessageRequest(UUID.randomUUID(), "Blocked attempt", null, null)))
                .isInstanceOf(BlockedUserException.class);

        // 3. Bob cannot send new messages while block relationship exists
        assertThatThrownBy(() -> messagingService.sendMessage(conv.id(), bob.getId(),
                new SendMessageRequest(UUID.randomUUID(), "Bob attempt while blocking", null, null)))
                .isInstanceOf(BlockedUserException.class);

        // 4. Reactions are rejected
        assertThatThrownBy(() -> messagingService.addOrUpdateReaction(m1.id(), bob.getId(), new ReactionRequest("👍")))
                .isInstanceOf(BlockedUserException.class);

        // 5. Typing is rejected
        assertThatThrownBy(() -> typingService.handleTyping(conv.id(), alice.getId(), "alice", true))
                .isInstanceOf(BlockedUserException.class);

        // 6. Unblock restores send eligibility
        userBlockRepository.delete(block);
        userBlockRepository.flush();

        MessageResponse unblockedMsg = messagingService.sendMessage(conv.id(), alice.getId(),
                new SendMessageRequest(UUID.randomUUID(), "After unblock message", null, null));
        assertThat(unblockedMsg).isNotNull();
        assertThat(unblockedMsg.body()).isEqualTo("After unblock message");

        // History remains intact (now 2 messages)
        MessagePageResponse restoredHistory = messagingService.getMessages(conv.id(), bob.getId(), null, 10);
        assertThat(restoredHistory.items()).hasSize(2);
    }

    @Test
    @DisplayName("Keyset Pagination: Conversations and Messages page correctly with nextCursor")
    void keysetPagination_worksCorrectly() {
        ConversationResponse conv = messagingService.getOrCreateDirectConversation(alice.getId(), bob.getId());

        // Send 5 messages
        for (int i = 1; i <= 5; i++) {
            messagingService.sendMessage(conv.id(), alice.getId(),
                    new SendMessageRequest(UUID.randomUUID(), "Message #" + i, null, null));
        }

        // Fetch page 1 (limit 2)
        MessagePageResponse page1 = messagingService.getMessages(conv.id(), alice.getId(), null, 2);
        assertThat(page1.items()).hasSize(2);
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.nextCursor()).isNotNull();

        // Fetch page 2
        MessagePageResponse page2 = messagingService.getMessages(conv.id(), alice.getId(), page1.nextCursor(), 2);
        assertThat(page2.items()).hasSize(2);
        assertThat(page2.hasMore()).isTrue();

        // Fetch page 3
        MessagePageResponse page3 = messagingService.getMessages(conv.id(), alice.getId(), page2.nextCursor(), 2);
        assertThat(page3.items()).hasSize(1);
        assertThat(page3.hasMore()).isFalse();
    }
}
