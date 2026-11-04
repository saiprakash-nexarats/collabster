package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.entity.Conversation;
import com.collabsphere_first.messaging.entity.ConversationParticipant;
import com.collabsphere_first.messaging.entity.ConversationParticipantId;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.exception.BlockedUserException;
import com.collabsphere_first.messaging.exception.ConversationNotFoundException;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.repository.ConversationParticipantRepository;
import com.collabsphere_first.messaging.repository.ConversationRepository;
import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingAccessServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;
    @Mock private NetworkAccessService networkAccessService;

    @InjectMocks
    private MessagingAccessServiceImpl accessService;

    private UUID conversationId;
    private final Long userA = 10L;
    private final Long userB = 20L;
    private final Long userC = 30L;

    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("requireParticipant throws when conversation not found")
    void requireParticipant_throwsWhenNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireParticipant(conversationId, userA))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("requireParticipant throws when user is not a participant")
    void requireParticipant_throwsWhenNotParticipant() {
        Conversation conv = Conversation.builder().id(conversationId).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, userC)).thenReturn(false);

        assertThatThrownBy(() -> accessService.requireParticipant(conversationId, userC))
                .isInstanceOf(MessagingAccessDeniedException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    @DisplayName("requireParticipant succeeds when user is a participant")
    void requireParticipant_succeeds() {
        Conversation conv = Conversation.builder().id(conversationId).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(true);

        Conversation result = accessService.requireParticipant(conversationId, userA);
        assertThat(result).isSameAs(conv);
    }

    @Test
    @DisplayName("validateCanInitiateDirectConversation rejects messaging self")
    void validateCanInitiate_rejectsSelf() {
        assertThatThrownBy(() -> accessService.validateCanInitiateDirectConversation(userA, userA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot create a direct conversation with yourself");
    }

    @Test
    @DisplayName("validateCanInitiateDirectConversation rejects nonexistent target")
    void validateCanInitiate_rejectsNonexistentTarget() {
        when(userRepository.findById(userB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.validateCanInitiateDirectConversation(userA, userB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target user not found");
    }

    @Test
    @DisplayName("validateCanInitiateDirectConversation rejects disabled target")
    void validateCanInitiate_rejectsDisabledTarget() {
        User disabled = User.builder().id(userB).accountEnabled(false).build();
        when(userRepository.findById(userB)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> accessService.validateCanInitiateDirectConversation(userA, userB))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account is disabled");
    }

    @Test
    @DisplayName("validateCanInitiateDirectConversation rejects blocked target")
    void validateCanInitiate_rejectsBlockedTarget() {
        User enabled = User.builder().id(userB).accountEnabled(true).build();
        when(userRepository.findById(userB)).thenReturn(Optional.of(enabled));
        when(networkAccessService.isBlockedEitherDirection(userA, userB)).thenReturn(true);

        assertThatThrownBy(() -> accessService.validateCanInitiateDirectConversation(userA, userB))
                .isInstanceOf(BlockedUserException.class)
                .hasMessageContaining("block relationship exists");
    }

    @Test
    @DisplayName("validateCanInitiateDirectConversation succeeds for eligible user")
    void validateCanInitiate_succeeds() {
        User enabled = User.builder().id(userB).accountEnabled(true).build();
        when(userRepository.findById(userB)).thenReturn(Optional.of(enabled));
        when(networkAccessService.isBlockedEitherDirection(userA, userB)).thenReturn(false);

        accessService.validateCanInitiateDirectConversation(userA, userB);
        verify(networkAccessService).isBlockedEitherDirection(userA, userB);
    }

    @Test
    @DisplayName("validateCanSendMessage rejects when block relationship exists")
    void validateCanSendMessage_rejectsWhenBlocked() {
        Conversation conv = Conversation.builder().id(conversationId).type(ConversationType.DIRECT).build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(true);

        ConversationParticipant p1 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, userA)).build();
        ConversationParticipant p2 = ConversationParticipant.builder()
                .id(new ConversationParticipantId(conversationId, userB)).build();
        when(participantRepository.findByIdConversationId(conversationId)).thenReturn(List.of(p1, p2));
        when(networkAccessService.isBlockedEitherDirection(userA, userB)).thenReturn(true);

        assertThatThrownBy(() -> accessService.validateCanSendMessage(conversationId, userA))
                .isInstanceOf(BlockedUserException.class)
                .hasMessageContaining("block relationship exists");
    }
}
