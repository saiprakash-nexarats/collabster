package com.collabsphere_first.messaging.controller;

import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.enums.ConversationType;
import com.collabsphere_first.messaging.enums.MessageDeliveryState;
import com.collabsphere_first.messaging.enums.MessageStatus;
import com.collabsphere_first.messaging.enums.MessageType;
import com.collabsphere_first.messaging.exception.*;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagingController MockMvc REST Tests")
class MessagingControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private MessagingService messagingService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MessagingController messagingController;

    private User currentUser;
    private final Long currentUserId = 100L;
    private final UUID conversationId = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messagingController)
                .setControllerAdvice(new MessagingExceptionHandler())
                .build();

        currentUser = User.builder().id(currentUserId).username("alice").email("alice@example.com").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    @DisplayName("POST /api/v1/messages/conversations/direct/{targetUserId} returns 201 Created")
    void createDirectConversation_returns201() throws Exception {
        ConversationResponse response = new ConversationResponse(
                conversationId,
                ConversationType.DIRECT,
                new ParticipantSummary(200L, "bob", "Bob Smith", null, true),
                null,
                null,
                0,
                false,
                false,
                Instant.now(),
                Instant.now()
        );

        when(messagingService.getOrCreateDirectConversation(currentUserId, 200L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/messages/conversations/direct/{targetUserId}", 200L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.data.type").value("DIRECT"));
    }

    @Test
    @DisplayName("GET /api/v1/messages/conversations returns 200 OK")
    void getConversations_returns200() throws Exception {
        ConversationPageResponse response = new ConversationPageResponse(List.of(), null, false);
        when(messagingService.getConversations(eq(currentUserId), any(), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/messages/conversations/{conversationId}/messages returns 201 Created")
    void sendMessage_returns201() throws Exception {
        SendMessageRequest request = new SendMessageRequest(UUID.randomUUID(), "Hello Bob", null, null);
        MessageResponse response = new MessageResponse(
                messageId,
                conversationId,
                currentUserId,
                "alice",
                request.clientMessageId(),
                MessageType.TEXT,
                "Hello Bob",
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

        when(messagingService.sendMessage(eq(conversationId), eq(currentUserId), any(SendMessageRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(messageId.toString()))
                .andExpect(jsonPath("$.data.body").value("Hello Bob"));
    }

    @Test
    @DisplayName("PATCH /api/v1/messages/{messageId} returns 200 OK")
    void editMessage_returns200() throws Exception {
        EditMessageRequest request = new EditMessageRequest("Updated content");
        MessageResponse response = new MessageResponse(
                messageId,
                conversationId,
                currentUserId,
                "alice",
                null,
                MessageType.TEXT,
                "Updated content",
                List.of(),
                null,
                List.of(),
                MessageDeliveryState.SENT,
                MessageStatus.ACTIVE,
                false,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        when(messagingService.editMessage(eq(messageId), eq(currentUserId), any(EditMessageRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.body").value("Updated content"));
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{messageId} returns 200 OK")
    void deleteMessage_returns200() throws Exception {
        doNothing().when(messagingService).deleteMessage(messageId, currentUserId);

        mockMvc.perform(delete("/api/v1/messages/{messageId}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(messagingService).deleteMessage(messageId, currentUserId);
    }

    @Test
    @DisplayName("PUT /api/v1/messages/{messageId}/reaction returns 200 OK")
    void addReaction_returns200() throws Exception {
        ReactionRequest request = new ReactionRequest("❤️");
        ReactionResponse response = new ReactionResponse(UUID.randomUUID(), currentUserId, "alice", "❤️", Instant.now());

        when(messagingService.addOrUpdateReaction(eq(messageId), eq(currentUserId), any(ReactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/messages/{messageId}/reaction", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reaction").value("❤️"));
    }

    @Test
    @DisplayName("POST /api/v1/messages/conversations/{conversationId}/read returns 200 OK")
    void markRead_returns200() throws Exception {
        ReadReceiptRequest request = new ReadReceiptRequest(messageId);
        doNothing().when(messagingService).markConversationRead(eq(conversationId), eq(currentUserId), any(ReadReceiptRequest.class));

        mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/read", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Blocked user operation returns 403 Forbidden")
    void blockedUser_returns403() throws Exception {
        when(messagingService.getOrCreateDirectConversation(currentUserId, 300L))
                .thenThrow(new BlockedUserException("Cannot start conversation: a block relationship exists with this user"));

        mockMvc.perform(post("/api/v1/messages/conversations/direct/{targetUserId}", 300L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot start conversation: a block relationship exists with this user"));
    }

    @Test
    @DisplayName("Rate limit exceeded returns 429 Too Many Requests")
    void rateLimitExceeded_returns429() throws Exception {
        when(messagingService.sendMessage(eq(conversationId), eq(currentUserId), any(SendMessageRequest.class)))
                .thenThrow(new MessagingRateLimitExceededException("Rate limit exceeded for message-send. Try again in 30 seconds."));

        SendMessageRequest request = new SendMessageRequest(UUID.randomUUID(), "Spam", null, null);

        mockMvc.perform(post("/api/v1/messages/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded for message-send. Try again in 30 seconds."));
    }
}
