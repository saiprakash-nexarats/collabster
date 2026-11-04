package com.collabsphere_first.security.websocket;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionReaperLoadTest {

    private WebSocketAuthenticatedSessionRegistry registry;

    @Mock
    private MessagingPresenceService presenceService;

    @Mock
    private UserRepository userRepository;

    private WebSocketSessionReaper reaper;

    private final Instant baseTime = Instant.parse("2026-10-02T10:00:00Z");

    @BeforeEach
    void setUp() {
        registry = new WebSocketAuthenticatedSessionRegistry();
        registry.setClock(Clock.fixed(baseTime, ZoneOffset.UTC));
        reaper = new WebSocketSessionReaper(registry, presenceService, userRepository);
    }

    @Test
    @DisplayName("Section 45-47: Reaper processes 1,250 active sessions in safe 500-ID database chunks")
    void testReaperChunksLargeActiveUserSetSafely() {
        int totalUsers = 1250;
        Instant futureExpiry = baseTime.plus(Duration.ofHours(1));

        for (long i = 1; i <= totalUsers; i++) {
            registry.registerSession("sess-" + i, i, "user" + i, futureExpiry, 1L, "/ws/messages");
        }

        assertThat(registry.getAllActiveUserIds()).hasSize(totalUsers);

        when(userRepository.findAuthStatesByUserIds(anySet())).thenAnswer(invocation -> {
            Set<Long> requestedIds = invocation.getArgument(0);
            List<Object[]> rows = new ArrayList<>();
            for (Long uid : requestedIds) {
                // All users have auth_version = 1, enabled=true, deactivated=false, deleted=false
                rows.add(new Object[]{uid, 1L, true, false, false});
            }
            return rows;
        });

        long startNanos = System.nanoTime();
        reaper.reapRevokedSessions();
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        // With 1,250 users and chunk size 500: exactly 3 chunked database queries executed (500 + 500 + 250)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> captor = ArgumentCaptor.forClass(Set.class);
        verify(userRepository, times(3)).findAuthStatesByUserIds(captor.capture());

        List<Set<Long>> capturedChunks = captor.getAllValues();
        assertThat(capturedChunks.get(0)).hasSize(500);
        assertThat(capturedChunks.get(1)).hasSize(500);
        assertThat(capturedChunks.get(2)).hasSize(250);

        // Verification: sweep duration on 1,250 sessions completes in milliseconds, well within 5000ms cadence
        assertThat(durationMs).isLessThan(1000);
    }

    @Test
    @DisplayName("Section 43: Periodic pruneRevokedSessionIds purges expired entries to prevent memory leak")
    void testPruneRevokedSessionIdsBoundsMemory() {
        // Register sessions for user 42
        registry.registerSession("sess-old-1", 42L, "alice", baseTime.plus(Duration.ofHours(1)), 1L, "/ws/messages");
        registry.registerSession("sess-old-2", 42L, "alice", baseTime.plus(Duration.ofHours(1)), 1L, "/ws/messages");

        // Revoke older sessions
        registry.closeSessionsOlderThanAuthVersion(42L, 2L, "Logout all");

        // Before prune: revoked ID check still knows it was revoked
        assertThat(registry.isRevoked("sess-old-1")).isTrue();

        // Fast forward clock by 31 minutes (exceeding the 30-minute retention TTL)
        registry.setClock(Clock.fixed(baseTime.plus(Duration.ofMinutes(31)), ZoneOffset.UTC));

        // Run periodic prune
        registry.pruneRevokedSessionIds();

        // Expired entry is pruned from map
        assertThat(registry.isRevoked("sess-old-1")).isFalse();
    }
}
