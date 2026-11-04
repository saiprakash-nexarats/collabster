package com.collabsphere_first.network;

import com.collabsphere_first.network.dto.SocialCountsResponse;
import com.collabsphere_first.network.repository.UserBlockRepository;
import com.collabsphere_first.network.repository.UserConnectionRepository;
import com.collabsphere_first.network.repository.UserFollowRepository;
import com.collabsphere_first.network.repository.UserMuteRepository;
import com.collabsphere_first.network.service.NetworkAccessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NetworkAccessService Cross-Domain Query Tests")
class NetworkAccessServiceTest {

    @Mock
    private UserFollowRepository userFollowRepository;

    @Mock
    private UserConnectionRepository userConnectionRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserMuteRepository userMuteRepository;

    @InjectMocks
    private NetworkAccessServiceImpl networkAccessService;

    @Test
    @DisplayName("isFollowing delegates to UserFollowRepository")
    void isFollowing_Success() {
        when(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).thenReturn(true);
        assertTrue(networkAccessService.isFollowing(1L, 2L));

        when(userFollowRepository.existsByFollowerIdAndFollowingId(1L, 3L)).thenReturn(false);
        assertFalse(networkAccessService.isFollowing(1L, 3L));

        assertFalse(networkAccessService.isFollowing(null, 2L));
        assertFalse(networkAccessService.isFollowing(1L, null));
    }

    @Test
    @DisplayName("isConnected delegates to UserConnectionRepository")
    void isConnected_Success() {
        when(userConnectionRepository.isConnected(1L, 2L)).thenReturn(true);
        assertTrue(networkAccessService.isConnected(1L, 2L));

        when(userConnectionRepository.isConnected(1L, 3L)).thenReturn(false);
        assertFalse(networkAccessService.isConnected(1L, 3L));

        assertFalse(networkAccessService.isConnected(null, 2L));
    }

    @Test
    @DisplayName("isBlocked delegates to UserBlockRepository")
    void isBlocked_Success() {
        when(userBlockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(true);
        assertTrue(networkAccessService.isBlocked(1L, 2L));

        when(userBlockRepository.isBlockedEitherDirection(1L, 2L)).thenReturn(true);
        assertTrue(networkAccessService.isBlockedEitherDirection(1L, 2L));

        assertFalse(networkAccessService.isBlocked(null, 2L));
        assertFalse(networkAccessService.isBlockedEitherDirection(null, 2L));
    }

    @Test
    @DisplayName("isMuted delegates to UserMuteRepository")
    void isMuted_Success() {
        when(userMuteRepository.existsByMuterIdAndMutedId(1L, 2L)).thenReturn(true);
        assertTrue(networkAccessService.isMuted(1L, 2L));

        when(userMuteRepository.existsByMuterIdAndMutedId(1L, 3L)).thenReturn(false);
        assertFalse(networkAccessService.isMuted(1L, 3L));

        assertFalse(networkAccessService.isMuted(null, 2L));
    }

    @Test
    @DisplayName("getSocialCounts retrieves counts efficiently")
    void getSocialCounts_Success() {
        when(userFollowRepository.countByFollowingId(1L)).thenReturn(100L);
        when(userFollowRepository.countByFollowerId(1L)).thenReturn(50L);
        when(userConnectionRepository.countAcceptedConnections(1L)).thenReturn(25L);

        SocialCountsResponse counts = networkAccessService.getSocialCounts(1L);

        assertEquals(100L, counts.getFollowersCount());
        assertEquals(50L, counts.getFollowingCount());
        assertEquals(25L, counts.getConnectionsCount());
        assertEquals(100L, networkAccessService.getFollowersCount(1L));
        assertEquals(50L, networkAccessService.getFollowingCount(1L));
        assertEquals(25L, networkAccessService.getConnectionsCount(1L));
    }
}
