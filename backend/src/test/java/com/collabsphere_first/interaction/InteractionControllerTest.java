package com.collabsphere_first.interaction;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.interaction.controller.InteractionController;
import com.collabsphere_first.interaction.dto.*;
import com.collabsphere_first.interaction.enums.InteractionEventType;
import com.collabsphere_first.interaction.exception.ContentInaccessibleException;
import com.collabsphere_first.interaction.exception.InteractionExceptionHandler;
import com.collabsphere_first.interaction.exception.InvalidInteractionException;
import com.collabsphere_first.interaction.service.InteractionService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InteractionController REST API Tests")
class InteractionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private InteractionService interactionService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private InteractionController interactionController;

    private User currentUser;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interactionController)
                .setControllerAdvice(new InteractionExceptionHandler())
                .build();

        currentUser = User.builder().id(100L).firstName("Current").lastName("User").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("PUT /{contentType}/{contentId}/like returns 200 OK")
    void likeContent_Returns200() throws Exception {
        LikeResponse response = LikeResponse.builder()
                .contentRef(ContentRef.platformPost(contentId))
                .liked(true)
                .likeCount(5)
                .build();
        when(interactionService.likeContent(eq(currentUser), any(ContentRef.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/interactions/PLATFORM_POST/" + contentId + "/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(5));
    }

    @Test
    @DisplayName("DELETE /{contentType}/{contentId}/like returns 200 OK")
    void unlikeContent_Returns200() throws Exception {
        LikeResponse response = LikeResponse.builder()
                .contentRef(ContentRef.platformPost(contentId))
                .liked(false)
                .likeCount(4)
                .build();
        when(interactionService.unlikeContent(eq(currentUser), any(ContentRef.class))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/interactions/PLATFORM_POST/" + contentId + "/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(4));
    }

    @Test
    @DisplayName("PUT /{contentType}/{contentId}/save returns 200 OK")
    void saveContent_Returns200() throws Exception {
        SaveResponse response = SaveResponse.builder()
                .contentRef(ContentRef.platformPost(contentId))
                .saved(true)
                .saveCount(2)
                .build();
        when(interactionService.saveContent(eq(currentUser), any(ContentRef.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/interactions/PLATFORM_POST/" + contentId + "/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved").value(true));
    }

    @Test
    @DisplayName("GET /saved returns 200 OK with paginated saved items")
    void getSavedContent_Returns200() throws Exception {
        SavedItemsCursorPageResponse page = SavedItemsCursorPageResponse.builder()
                .items(List.of())
                .hasMore(false)
                .limit(20)
                .build();
        when(interactionService.getSavedContent(eq(currentUser), any(), eq(20))).thenReturn(page);

        mockMvc.perform(get("/api/v1/interactions/saved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /{contentType}/{contentId}/share returns 200 OK")
    void shareContent_Returns200() throws Exception {
        doNothing().when(interactionService).recordShare(eq(currentUser), any(ContentRef.class));

        mockMvc.perform(post("/api/v1/interactions/PLATFORM_POST/" + contentId + "/share"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /events returns 200 OK")
    void recordEvent_Returns200() throws Exception {
        InteractionEventRequest request = InteractionEventRequest.builder()
                .contentRef(ContentRef.platformPost(contentId))
                .eventType(InteractionEventType.WATCH_TIME)
                .durationMs(5000L)
                .build();

        doNothing().when(interactionService).recordEvent(eq(currentUser), any());

        mockMvc.perform(post("/api/v1/interactions/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /events/batch returns 200 OK with recorded count")
    void recordBatchEvents_Returns200() throws Exception {
        InteractionEventRequest e1 = InteractionEventRequest.builder()
                .contentRef(ContentRef.platformPost(contentId))
                .eventType(InteractionEventType.VIEW)
                .build();
        BatchInteractionEventRequest batchReq = BatchInteractionEventRequest.builder()
                .events(List.of(e1))
                .build();

        when(interactionService.recordBatchEvents(eq(currentUser), any())).thenReturn(1);

        mockMvc.perform(post("/api/v1/interactions/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordedCount").value(1));
    }

    @Test
    @DisplayName("GET /{contentType}/{contentId}/status returns status map")
    void getStatus_Returns200() throws Exception {
        when(interactionService.isLiked(eq(100L), any())).thenReturn(true);
        when(interactionService.isSaved(eq(100L), any())).thenReturn(false);
        when(interactionService.getLikeCount(any())).thenReturn(10L);
        when(interactionService.getSaveCount(any())).thenReturn(3L);

        mockMvc.perform(get("/api/v1/interactions/PLATFORM_POST/" + contentId + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.saved").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(10))
                .andExpect(jsonPath("$.data.saveCount").value(3));
    }

    @Test
    @DisplayName("Inaccessible content returns 403 Forbidden")
    void inaccessibleContent_Returns403() throws Exception {
        when(interactionService.likeContent(eq(currentUser), any()))
                .thenThrow(new ContentInaccessibleException("Content not accessible"));

        mockMvc.perform(put("/api/v1/interactions/PLATFORM_POST/" + contentId + "/like"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Content not accessible"));
    }

    @Test
    @DisplayName("Invalid interaction parameter returns 400 Bad Request")
    void invalidInteraction_Returns400() throws Exception {
        when(interactionService.likeContent(eq(currentUser), any()))
                .thenThrow(new InvalidInteractionException("Invalid contentRef"));

        mockMvc.perform(put("/api/v1/interactions/PLATFORM_POST/" + contentId + "/like"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid contentRef"));
    }
}
