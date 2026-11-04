package com.collabsphere_first.network;

import com.collabsphere_first.network.controller.NetworkController;
import com.collabsphere_first.network.dto.*;
import com.collabsphere_first.network.enums.ConnectionStatus;
import com.collabsphere_first.network.exception.*;
import com.collabsphere_first.network.service.NetworkService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NetworkController REST API Tests")
class NetworkControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NetworkService networkService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private NetworkController networkController;

    private User currentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(networkController)
                .setControllerAdvice(new NetworkExceptionHandler())
                .build();

        currentUser = User.builder().id(100L).firstName("Current").lastName("User").username("curruser").build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(currentUser);
    }

    // ==========================================
    // FOLLOW
    // ==========================================
    @Test
    @DisplayName("POST /api/v1/network/follows/{targetUserId} returns 201 Created")
    void followUser_Returns201() throws Exception {
        FollowSummaryResponse response = FollowSummaryResponse.builder()
                .user(UserSummaryDto.builder().id(200L).username("target").build())
                .followedAt(Instant.now())
                .build();
        when(networkService.followUser(100L, 200L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/network/follows/200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.id").value(200));
    }

    @Test
    @DisplayName("DELETE /api/v1/network/follows/{targetUserId} returns 200 OK")
    void unfollowUser_Returns200() throws Exception {
        doNothing().when(networkService).unfollowUser(100L, 200L);

        mockMvc.perform(delete("/api/v1/network/follows/200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/network/followers/me returns 200 OK")
    void getMyFollowers_Returns200() throws Exception {
        NetworkCursorPageResponse<FollowSummaryResponse> page = NetworkCursorPageResponse.<FollowSummaryResponse>builder()
                .items(List.of())
                .hasMore(false)
                .limit(20)
                .build();
        when(networkService.getFollowers(100L, null, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/network/followers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ==========================================
    // CONNECTION
    // ==========================================
    @Test
    @DisplayName("POST /api/v1/network/connections/requests/{targetUserId} returns 201 Created")
    void sendConnectionRequest_Returns201() throws Exception {
        UUID reqId = UUID.randomUUID();
        ConnectionRequestResponse response = ConnectionRequestResponse.builder()
                .id(reqId)
                .requester(UserSummaryDto.builder().id(100L).build())
                .addressee(UserSummaryDto.builder().id(200L).build())
                .status(ConnectionStatus.PENDING)
                .build();
        when(networkService.sendConnectionRequest(100L, 200L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/network/connections/requests/200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/network/connections/requests/{requestId}/accept returns 200 OK")
    void acceptConnectionRequest_Returns200() throws Exception {
        UUID reqId = UUID.randomUUID();
        ConnectionRequestResponse response = ConnectionRequestResponse.builder()
                .id(reqId)
                .status(ConnectionStatus.ACCEPTED)
                .build();
        when(networkService.acceptConnectionRequest(100L, reqId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/network/connections/requests/" + reqId + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/network/connections/requests/{requestId} returns 200 OK")
    void cancelConnectionRequest_Returns200() throws Exception {
        UUID reqId = UUID.randomUUID();
        doNothing().when(networkService).cancelConnectionRequest(100L, reqId);

        mockMvc.perform(delete("/api/v1/network/connections/requests/" + reqId))
                .andExpect(status().isOk());
    }

    // ==========================================
    // BLOCK / MUTE
    // ==========================================
    @Test
    @DisplayName("PUT /api/v1/network/blocks/{targetUserId} returns 200 OK")
    void blockUser_Returns200() throws Exception {
        doNothing().when(networkService).blockUser(100L, 200L);

        mockMvc.perform(put("/api/v1/network/blocks/200"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/network/mutes/{targetUserId} returns 200 OK")
    void muteUser_Returns200() throws Exception {
        doNothing().when(networkService).muteUser(100L, 200L);

        mockMvc.perform(put("/api/v1/network/mutes/200"))
                .andExpect(status().isOk());
    }

    // ==========================================
    // COUNTS & EXCEPTIONS
    // ==========================================
    @Test
    @DisplayName("GET /api/v1/network/counts/me returns social counts")
    void getMyCounts_Returns200() throws Exception {
        SocialCountsResponse counts = new SocialCountsResponse(10, 5, 3);
        when(networkService.getSocialCounts(100L)).thenReturn(counts);

        mockMvc.perform(get("/api/v1/network/counts/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.followersCount").value(10))
                .andExpect(jsonPath("$.data.followingCount").value(5))
                .andExpect(jsonPath("$.data.connectionsCount").value(3));
    }

    @Test
    @DisplayName("Returns 400 Bad Request when self-action is attempted")
    void selfFollow_Returns400() throws Exception {
        when(networkService.followUser(100L, 100L)).thenThrow(new InvalidRelationshipException("Cannot follow yourself"));

        mockMvc.perform(post("/api/v1/network/follows/100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot follow yourself"));
    }

    @Test
    @DisplayName("Returns 403 Forbidden when action is blocked")
    void blockedFollow_Returns403() throws Exception {
        when(networkService.followUser(100L, 200L)).thenThrow(new NetworkAccessDeniedException("Active block relationship"));

        mockMvc.perform(post("/api/v1/network/follows/200"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Active block relationship"));
    }

    @Test
    @DisplayName("Returns 409 Conflict when relationship conflict occurs")
    void alreadyConnected_Returns409() throws Exception {
        when(networkService.sendConnectionRequest(100L, 200L))
                .thenThrow(new RelationshipConflictException("Already connected"));

        mockMvc.perform(post("/api/v1/network/connections/requests/200"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already connected"));
    }
}
