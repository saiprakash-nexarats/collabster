package com.collabsphere_first.network;

import com.collabsphere_first.network.dto.*;
import com.collabsphere_first.network.entity.*;
import com.collabsphere_first.network.enums.ConnectionStatus;
import com.collabsphere_first.network.event.ConnectionAcceptedEvent;
import com.collabsphere_first.network.event.ConnectionRequestedEvent;
import com.collabsphere_first.network.event.UserFollowedEvent;
import com.collabsphere_first.network.exception.*;
import com.collabsphere_first.network.repository.*;
import com.collabsphere_first.network.service.NetworkServiceImpl;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NetworkService Domain Tests")
class NetworkServiceTest {

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserConnectionRepository userConnectionRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserMuteRepository userMuteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NetworkServiceImpl networkService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).firstName("Alice").lastName("Smith").username("alice").build();
        bob = User.builder().id(2L).firstName("Bob").lastName("Jones").username("bob").build();
    }

    // ==========================================
    // FOLLOW TESTS
    // ==========================================
    @Nested
    @DisplayName("Follow Operation Tests")
    class FollowTests {

        @Test
        @DisplayName("Successfully follow another user")
        void followUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);
            when(userFollowRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

            UserFollow savedFollow = UserFollow.builder()
                    .id(UUID.randomUUID())
                    .follower(alice)
                    .following(bob)
                    .createdAt(Instant.now())
                    .build();
            when(userFollowRepository.save(any(UserFollow.class))).thenReturn(savedFollow);

            FollowSummaryResponse response = networkService.followUser(1L, 2L);

            assertNotNull(response);
            assertEquals(2L, response.getUser().getId());
            verify(userFollowRepository).save(any(UserFollow.class));
            verify(eventPublisher).publishEvent(any(UserFollowedEvent.class));
        }

        @Test
        @DisplayName("Duplicate follow is idempotent")
        void followUser_Duplicate_Idempotent() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);

            UserFollow existingFollow = UserFollow.builder()
                    .id(UUID.randomUUID())
                    .follower(alice)
                    .following(bob)
                    .createdAt(Instant.now())
                    .build();
            when(userFollowRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(existingFollow));

            FollowSummaryResponse response = networkService.followUser(1L, 2L);

            assertNotNull(response);
            assertEquals(2L, response.getUser().getId());
            verify(userFollowRepository, never()).save(any(UserFollow.class));
        }

        @Test
        @DisplayName("Self follow is rejected with 400 Bad Request")
        void followUser_SelfFollow_Rejected() {
            assertThrows(InvalidRelationshipException.class, () -> networkService.followUser(1L, 1L));
            verify(userFollowRepository, never()).save(any());
        }

        @Test
        @DisplayName("Follow blocked user is rejected with 403 Forbidden")
        void followUser_BlockedUser_Rejected() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(true);

            assertThrows(NetworkAccessDeniedException.class, () -> networkService.followUser(1L, 2L));
            verify(userFollowRepository, never()).save(any());
        }

        @Test
        @DisplayName("Follow non-existent user throws 404")
        void followUser_TargetNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(RelationshipNotFoundException.class, () -> networkService.followUser(1L, 999L));
        }

        @Test
        @DisplayName("Unfollow user removes follow record")
        void unfollowUser_Success() {
            networkService.unfollowUser(1L, 2L);
            verify(userFollowRepository).deleteByFollowerIdAndFollowingId(1L, 2L);
        }
    }

    // ==========================================
    // CONNECTION TESTS
    // ==========================================
    @Nested
    @DisplayName("Connection Lifecycle Tests")
    class ConnectionTests {

        @Test
        @DisplayName("Send connection request creates PENDING relationship")
        void sendConnectionRequest_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);
            when(userConnectionRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());

            UserConnection savedConn = UserConnection.builder()
                    .id(UUID.randomUUID())
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            when(userConnectionRepository.save(any(UserConnection.class))).thenReturn(savedConn);

            ConnectionRequestResponse response = networkService.sendConnectionRequest(1L, 2L);

            assertNotNull(response);
            assertEquals(ConnectionStatus.PENDING, response.getStatus());
            assertEquals(1L, response.getRequester().getId());
            assertEquals(2L, response.getAddressee().getId());
            verify(eventPublisher).publishEvent(any(ConnectionRequestedEvent.class));
        }

        @Test
        @DisplayName("Reverse pending request auto-accepts connection (reciprocal interest)")
        void sendConnectionRequest_ReversePending_AutoAccepts() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);

            UserConnection reversePending = UserConnection.builder()
                    .id(UUID.randomUUID())
                    .requester(bob)
                    .addressee(alice)
                    .status(ConnectionStatus.PENDING)
                    .createdAt(Instant.now().minusSeconds(100))
                    .build();
            when(userConnectionRepository.findBetween(1L, 2L)).thenReturn(Optional.of(reversePending));
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArgument(0));

            ConnectionRequestResponse response = networkService.sendConnectionRequest(1L, 2L);

            assertNotNull(response);
            assertEquals(ConnectionStatus.ACCEPTED, response.getStatus());
            verify(eventPublisher).publishEvent(any(ConnectionAcceptedEvent.class));
        }

        @Test
        @DisplayName("Already connected users cannot send new connection request")
        void sendConnectionRequest_AlreadyConnected_ThrowsConflict() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);

            UserConnection existing = UserConnection.builder()
                    .id(UUID.randomUUID())
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();
            when(userConnectionRepository.findBetween(1L, 2L)).thenReturn(Optional.of(existing));

            assertThrows(RelationshipConflictException.class, () -> networkService.sendConnectionRequest(1L, 2L));
        }

        @Test
        @DisplayName("Addressee successfully accepts connection request")
        void acceptConnectionRequest_Success() {
            UUID reqId = UUID.randomUUID();
            UserConnection conn = UserConnection.builder()
                    .id(reqId)
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

            when(userConnectionRepository.findById(reqId)).thenReturn(Optional.of(conn));
            when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(false);
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArgument(0));

            ConnectionRequestResponse response = networkService.acceptConnectionRequest(2L, reqId);

            assertNotNull(response);
            assertEquals(ConnectionStatus.ACCEPTED, response.getStatus());
            verify(eventPublisher).publishEvent(any(ConnectionAcceptedEvent.class));
        }

        @Test
        @DisplayName("Non-addressee cannot accept someone else's connection request (IDOR check)")
        void acceptConnectionRequest_UnauthorizedUser_ThrowsForbidden() {
            UUID reqId = UUID.randomUUID();
            UserConnection conn = UserConnection.builder()
                    .id(reqId)
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.PENDING)
                    .build();

            when(userConnectionRepository.findById(reqId)).thenReturn(Optional.of(conn));

            assertThrows(NetworkAccessDeniedException.class, () -> networkService.acceptConnectionRequest(3L, reqId));
            verify(userConnectionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Addressee declines connection request")
        void declineConnectionRequest_Success() {
            UUID reqId = UUID.randomUUID();
            UserConnection conn = UserConnection.builder()
                    .id(reqId)
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.PENDING)
                    .build();

            when(userConnectionRepository.findById(reqId)).thenReturn(Optional.of(conn));
            when(userConnectionRepository.save(any(UserConnection.class))).thenAnswer(i -> i.getArgument(0));

            ConnectionRequestResponse response = networkService.declineConnectionRequest(2L, reqId);

            assertNotNull(response);
            assertEquals(ConnectionStatus.DECLINED, response.getStatus());
        }

        @Test
        @DisplayName("Requester cancels pending connection request")
        void cancelConnectionRequest_Success() {
            UUID reqId = UUID.randomUUID();
            UserConnection conn = UserConnection.builder()
                    .id(reqId)
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.PENDING)
                    .build();

            when(userConnectionRepository.findById(reqId)).thenReturn(Optional.of(conn));

            networkService.cancelConnectionRequest(1L, reqId);

            assertEquals(ConnectionStatus.CANCELLED, conn.getStatus());
            verify(userConnectionRepository).save(conn);
        }

        @Test
        @DisplayName("Disconnect removes accepted connection")
        void disconnect_Success() {
            UserConnection conn = UserConnection.builder()
                    .id(UUID.randomUUID())
                    .requester(alice)
                    .addressee(bob)
                    .status(ConnectionStatus.ACCEPTED)
                    .build();
            when(userConnectionRepository.findBetweenWithStatus(1L, 2L, ConnectionStatus.ACCEPTED)).thenReturn(Optional.of(conn));

            networkService.disconnect(1L, 2L);

            verify(userConnectionRepository).delete(conn);
        }
    }

    // ==========================================
    // BLOCK TESTS
    // ==========================================
    @Nested
    @DisplayName("Block Operation & Cascading Tests")
    class BlockTests {

        @Test
        @DisplayName("Blocking a user creates block and severs all follows and connections")
        void blockUser_CascadesCleanup() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false);

            networkService.blockUser(1L, 2L);

            verify(userBlockRepository).save(any(UserBlock.class));
            verify(userFollowRepository).deleteAllFollowsBetween(1L, 2L);
            verify(userConnectionRepository).deleteAllConnectionsBetween(1L, 2L);
        }

        @Test
        @DisplayName("Duplicate block is idempotent")
        void blockUser_Duplicate_Idempotent() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);

            networkService.blockUser(1L, 2L);

            verify(userBlockRepository, never()).save(any(UserBlock.class));
            verify(userFollowRepository).deleteAllFollowsBetween(1L, 2L);
            verify(userConnectionRepository).deleteAllConnectionsBetween(1L, 2L);
        }

        @Test
        @DisplayName("Self block is rejected")
        void blockUser_SelfBlock_Rejected() {
            assertThrows(InvalidRelationshipException.class, () -> networkService.blockUser(1L, 1L));
            verify(userBlockRepository, never()).save(any());
        }

        @Test
        @DisplayName("Unblock removes block record")
        void unblockUser_Success() {
            networkService.unblockUser(1L, 2L);
            verify(userBlockRepository).deleteByBlockerIdAndBlockedId(1L, 2L);
        }
    }

    // ==========================================
    // MUTE TESTS
    // ==========================================
    @Nested
    @DisplayName("Mute Operation Tests")
    class MuteTests {

        @Test
        @DisplayName("Mute user creates private mute record without affecting follows or connections")
        void muteUser_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
            when(userMuteRepository.existsByMuterIdAndMutedId(1L, 2L)).thenReturn(false);

            networkService.muteUser(1L, 2L);

            verify(userMuteRepository).save(any(UserMute.class));
            verify(userFollowRepository, never()).deleteAllFollowsBetween(any(), any());
            verify(userConnectionRepository, never()).deleteAllConnectionsBetween(any(), any());
        }

        @Test
        @DisplayName("Self mute is rejected")
        void muteUser_SelfMute_Rejected() {
            assertThrows(InvalidRelationshipException.class, () -> networkService.muteUser(1L, 1L));
        }

        @Test
        @DisplayName("Unmute removes mute record")
        void unmuteUser_Success() {
            networkService.unmuteUser(1L, 2L);
            verify(userMuteRepository).deleteByMuterIdAndMutedId(1L, 2L);
        }
    }

    // ==========================================
    // SOCIAL COUNTS
    // ==========================================
    @Nested
    @DisplayName("Social Counts Tests")
    class SocialCountsTests {

        @Test
        @DisplayName("Calculates social counts correctly")
        void getSocialCounts_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
            when(userFollowRepository.countByFollowingId(1L)).thenReturn(42L);
            when(userFollowRepository.countByFollowerId(1L)).thenReturn(15L);
            when(userConnectionRepository.countAcceptedConnections(1L)).thenReturn(8L);

            SocialCountsResponse counts = networkService.getSocialCounts(1L);

            assertEquals(42L, counts.getFollowersCount());
            assertEquals(15L, counts.getFollowingCount());
            assertEquals(8L, counts.getConnectionsCount());
        }
    }
}
