package com.collabsphere_first.messaging.security;

import com.collabsphere_first.messaging.controller.MessagingController;
import com.collabsphere_first.messaging.entity.*;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.exception.MessagingExceptionHandler;
import com.collabsphere_first.messaging.repository.*;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.messaging.service.MessagingServiceImpl;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Messaging Membership Authorization & Functional Negative Tests")
class MessagingMembershipAuthorizationTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private MessagingService messagingService;
    @Mock private CurrentUserService currentUserService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private DirectMessageRepository messageRepository;
    @Mock private StarredMessageRepository starredMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private MessagingAccessService accessService;

    private MessagingController messagingController;

    private final Long userA = 10L;
    private final Long userB = 20L;
    private final Long attacker = 999L;
    private final UUID convId = UUID.randomUUID();
    private final UUID msgId = UUID.randomUUID();

    private User attackerUser;
    private User alice;

    @BeforeEach
    void setUp() {
        messagingController = new MessagingController(
                messagingService,
                currentUserService,
                null,
                null
        );

        mockMvc = MockMvcBuilders.standaloneSetup(messagingController)
                .setControllerAdvice(new MessagingExceptionHandler())
                .build();

        attackerUser = User.builder().id(attacker).username("attacker").email("attacker@evil.com").build();
        alice = User.builder().id(userA).username("alice").email("alice@collobster.com").build();
    }

    @Nested
    @DisplayName("Controller Layer Membership Negative Tests")
    class ControllerMembershipTests {

        @Test
        @DisplayName("GET /conversations/{id}/members rejects uninvited caller with 403 Forbidden")
        void getMembers_unauthorizedCaller_returns403() throws Exception {
            when(currentUserService.getCurrentUser()).thenReturn(attackerUser);
            when(messagingService.getConversationMembers(convId, attacker))
                    .thenThrow(new MessagingAccessDeniedException("You are not a participant in this conversation"));

            mockMvc.perform(get("/api/v1/messages/conversations/{conversationId}/members", convId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You are not a participant in this conversation"));
        }

        @Test
        @DisplayName("POST /conversations/{id}/members rejects unauthorized caller with 403 Forbidden")
        void addMember_unauthorizedCaller_returns403() throws Exception {
            when(currentUserService.getCurrentUser()).thenReturn(attackerUser);
            doThrow(new MessagingAccessDeniedException("You are not a participant in this conversation"))
                    .when(messagingService).addConversationMember(eq(convId), eq(attacker), eq(50L));

            mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/members", convId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("profileId", 50L))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("DELETE /conversations/{id}/members/{profileId} rejects non-admin/non-participant caller with 403 Forbidden")
        void removeMember_unauthorizedCaller_returns403() throws Exception {
            when(currentUserService.getCurrentUser()).thenReturn(attackerUser);
            doThrow(new MessagingAccessDeniedException("You are not authorized to remove other members from this conversation"))
                    .when(messagingService).removeConversationMember(convId, attacker, userA);

            mockMvc.perform(delete("/api/v1/messages/conversations/{conversationId}/members/{profileId}", convId, userA))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Calling endpoints return 501 Not Implemented explicitly")
        void calls_return501NotImplemented() throws Exception {
            mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/calls", convId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("callType", "video"))))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.success").value(false));

            mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/calls/signal", convId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("type", "offer"))))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.success").value(false));

            mockMvc.perform(post("/api/v1/messages/calls/{callId}/end", "call-123"))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.success").value(false));

            mockMvc.perform(get("/api/v1/messages/calls/history"))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Service Layer Business Authorization & Invariant Enforcement")
    class ServiceMembershipTests {

        private MessagingServiceImpl serviceImpl;

        @BeforeEach
        void initService() {
            serviceImpl = new MessagingServiceImpl(
                    conversationRepository,
                    participantRepository,
                    messageRepository,
                    null,
                    null,
                    null,
                    userRepository,
                    starredMessageRepository,
                    accessService,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Test
        @DisplayName("addConversationMember rejects adding members to a 1:1 DIRECT conversation")
        void addMember_toDirectConversation_throwsException() {
            Conversation directConv = Conversation.builder()
                    .id(convId)
                    .type(ConversationType.DIRECT)
                    .build();

            when(accessService.requireParticipant(convId, userA)).thenReturn(directConv);

            assertThatThrownBy(() -> serviceImpl.addConversationMember(convId, userA, 30L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot add members to a 1:1 direct conversation");
        }

        @Test
        @DisplayName("addConversationMember rejects non-existent target profile ID")
        void addMember_targetNotFound_throwsException() {
            Conversation groupConv = Conversation.builder()
                    .id(convId)
                    .type(ConversationType.GROUP)
                    .build();

            when(accessService.requireParticipant(convId, userA)).thenReturn(groupConv);
            when(userRepository.findById(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> serviceImpl.addConversationMember(convId, userA, 9999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Target user does not exist");
        }

        @Test
        @DisplayName("removeConversationMember prevents non-admin member from kicking other members")
        void removeMember_nonAdminAttemptingToKickOther_throwsAccessDenied() {
            Conversation groupConv = Conversation.builder().id(convId).type(ConversationType.GROUP).build();
            when(accessService.requireParticipant(convId, userB)).thenReturn(groupConv);

            // Alice joined first (creator/admin), Bob joined second
            ConversationParticipant partAlice = ConversationParticipant.builder()
                    .id(new ConversationParticipantId(convId, userA))
                    .joinedAt(Instant.now().minusSeconds(3600))
                    .build();
            ConversationParticipant partBob = ConversationParticipant.builder()
                    .id(new ConversationParticipantId(convId, userB))
                    .joinedAt(Instant.now().minusSeconds(1800))
                    .build();

            when(participantRepository.findByIdConversationId(convId)).thenReturn(List.of(partAlice, partBob));

            // Bob attempts to remove Alice
            assertThatThrownBy(() -> serviceImpl.removeConversationMember(convId, userB, userA))
                    .isInstanceOf(MessagingAccessDeniedException.class)
                    .hasMessageContaining("not authorized to remove other members");
        }

        @Test
        @DisplayName("removeConversationMember allows member to remove themselves (leave conversation)")
        void removeMember_selfLeaving_allowed() {
            Conversation groupConv = Conversation.builder().id(convId).type(ConversationType.GROUP).build();
            when(accessService.requireParticipant(convId, userB)).thenReturn(groupConv);

            serviceImpl.removeConversationMember(convId, userB, userB);

            verify(participantRepository).deleteById(new ConversationParticipantId(convId, userB));
        }

        @Test
        @DisplayName("starMessage verifies conversation participant and persists StarredMessage")
        void starMessage_persistsCorrectly() {
            when(accessService.requireParticipant(convId, userA)).thenReturn(null);
            DirectMessage msg = DirectMessage.builder()
                    .id(msgId)
                    .conversationId(convId)
                    .senderId(userB)
                    .body("Important message")
                    .build();

            when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));
            when(starredMessageRepository.existsByUserIdAndMessageId(userA, msgId)).thenReturn(false);
            when(userRepository.getReferenceById(userA)).thenReturn(alice);

            serviceImpl.starMessage(convId, msgId, userA);

            verify(starredMessageRepository).save(any(StarredMessage.class));
        }

        @Test
        @DisplayName("starMessage rejects message from a different conversation")
        void starMessage_differentConversation_throwsException() {
            UUID differentConvId = UUID.randomUUID();
            when(accessService.requireParticipant(convId, userA)).thenReturn(null);
            DirectMessage msg = DirectMessage.builder()
                    .id(msgId)
                    .conversationId(differentConvId)
                    .senderId(userB)
                    .build();

            when(messageRepository.findById(msgId)).thenReturn(Optional.of(msg));

            assertThatThrownBy(() -> serviceImpl.starMessage(convId, msgId, userA))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Message does not belong to conversation");
        }

        @Test
        @DisplayName("unstarMessage deletes starred message for caller")
        void unstarMessage_deletes() {
            when(accessService.requireParticipant(convId, userA)).thenReturn(null);

            serviceImpl.unstarMessage(convId, msgId, userA);

            verify(starredMessageRepository).deleteByUserIdAndMessageId(userA, msgId);
        }
    }
}

