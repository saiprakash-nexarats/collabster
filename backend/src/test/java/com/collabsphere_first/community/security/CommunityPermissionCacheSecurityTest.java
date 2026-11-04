package com.collabsphere_first.community.security;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.event.MembershipLifecycleEvent;
import com.collabsphere_first.community.repository.MembershipRepository;
import com.collabsphere_first.community.service.CommunityMembershipCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CommunityPermissionCacheSecurityTest.TestCacheConfig.class)
class CommunityPermissionCacheSecurityTest {

    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "community-membership-snapshots",
                    "community-permissions"
            );
        }

        @Bean
        public MembershipRepository membershipRepository() {
            return mock(MembershipRepository.class);
        }

        @Bean
        public CommunityRoleService communityRoleService() {
            return mock(CommunityRoleService.class);
        }

        @Bean
        public CommunityPermissionEvaluator communityPermissionEvaluator(
                MembershipRepository repository,
                CommunityRoleService roleService,
                CommunityMembershipCacheService cacheService) {
            return new CommunityPermissionEvaluator(repository, roleService, cacheService);
        }

        @Bean
        public CommunityMembershipCacheService communityMembershipCacheService(
                MembershipRepository repository,
                CacheManager cacheManager) {
            return new CommunityMembershipCacheService(repository, cacheManager);
        }
    }

    @Autowired
    private CommunityPermissionEvaluator permissionEvaluator;

    @Autowired
    private CommunityMembershipCacheService membershipCacheService;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CommunityRoleService roleService;

    @Autowired
    private CacheManager cacheManager;

    private UUID communityA;
    private UUID communityB;
    private Long userId;

    @BeforeEach
    void setUp() {
        communityA = UUID.randomUUID();
        communityB = UUID.randomUUID();
        userId = 42L;

        cacheManager.getCache("community-membership-snapshots").clear();
        cacheManager.getCache("community-permissions").clear();
        reset(membershipRepository, roleService);
        when(roleService.isPlatformAdmin(anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("1. First permission lookup calls repository; 2. Cached lookup hits cache without repository call")
    void firstLookupCallsRepository_secondLookupHitsCache() {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        // First call evaluates and caches
        boolean firstCheck = permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER");
        assertThat(firstCheck).isTrue();
        verify(membershipRepository, times(1)).findByCommunityIdAndUserId(communityA, userId);

        // Second call should hit "community-permissions" cache
        boolean cachedCheck = permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER");
        assertThat(cachedCheck).isTrue();
        verify(membershipRepository, times(1)).findByCommunityIdAndUserId(communityA, userId);
    }

    @Test
    @DisplayName("3. Cross-role cache isolation: MEMBER result=true must NEVER satisfy ADMIN or OWNER check")
    void crossRoleCacheIsolation_memberDoesNotGrantAdminOrOwner() {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        // Evaluate MEMBER -> true
        boolean isMember = permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER");
        assertThat(isMember).isTrue();

        // Check ADMIN: must NOT be satisfied by the cached MEMBER result!
        boolean isAdmin = permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN");
        assertThat(isAdmin).isFalse();

        // Check OWNER: must NOT be satisfied by the cached MEMBER result!
        boolean isOwner = permissionEvaluator.hasCommunityRole(communityA, userId, "OWNER");
        assertThat(isOwner).isFalse();

        // Check MODERATOR: must NOT be satisfied by the cached MEMBER result!
        boolean isModerator = permissionEvaluator.hasCommunityRole(communityA, userId, "MODERATOR");
        assertThat(isModerator).isFalse();
    }

    @Test
    @DisplayName("4. Cross-community isolation: ADMIN in community A does NOT grant ADMIN in community B")
    void crossCommunityIsolation_rolesDoNotCrossCommunities() {
        Membership adminInA = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.ADMIN)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(adminInA));
        when(membershipRepository.findByCommunityIdAndUserId(communityB, userId))
                .thenReturn(Optional.empty());

        assertThat(permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN")).isTrue();
        assertThat(permissionEvaluator.hasCommunityRole(communityB, userId, "ADMIN")).isFalse();
    }

    @Test
    @DisplayName("5. Role change evicts permission cache and re-evaluates with new role")
    void roleChange_evictsPermissionCache() {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        // Initial check: Not admin
        assertThat(permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN")).isFalse();
        verify(membershipRepository, times(1)).findByCommunityIdAndUserId(communityA, userId);

        // Promoted to ADMIN
        member.setRole(MembershipRole.ADMIN);
        membershipCacheService.evictUser(communityA, userId);

        // Next check: Cache evicted, re-evaluates against repo and returns true
        assertThat(permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN")).isTrue();
        verify(membershipRepository, times(2)).findByCommunityIdAndUserId(communityA, userId);
    }

    @Test
    @DisplayName("6. Status change (BANNED, MUTED, LEFT, SUSPENDED) evicts cache and blocks access")
    void statusChange_evictsCacheAndBlocksAccess() {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        // Active member check succeeds
        assertThat(permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER")).isTrue();

        // Member is banned
        member.setStatus(MembershipStatus.BANNED);
        membershipCacheService.onMembershipLifecycle(new MembershipLifecycleEvent(
                this,
                communityA,
                userId,
                999L,
                MembershipLifecycleEvent.Type.MEMBER_BANNED,
                MembershipRole.MEMBER,
                MembershipRole.MEMBER,
                MembershipStatus.ACTIVE,
                MembershipStatus.BANNED));

        // Next check fails immediately and re-checks repo
        assertThat(permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER")).isFalse();
        verify(membershipRepository, times(2)).findByCommunityIdAndUserId(communityA, userId);
    }

    @Test
    @DisplayName("7. Separate cache spaces: Membership snapshot cache does not collide with permission cache")
    void cacheSpacesAreSegregated() {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MODERATOR)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        // Fetch snapshot
        var snapshot = membershipCacheService.find(communityA, userId);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.role()).isEqualTo(MembershipRole.MODERATOR);

        // Snapshot is in "community-membership-snapshots"
        var snapCache = cacheManager.getCache("community-membership-snapshots");
        assertThat(snapCache.get(communityA.toString() + ":" + userId)).isNotNull();

        // Permission cache is still empty for ADMIN
        var permCache = cacheManager.getCache("community-permissions");
        assertThat(permCache.get(communityA.toString() + ":" + userId + ":ADMIN")).isNull();

        // Evaluate permission
        boolean isAdmin = permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN");
        assertThat(isAdmin).isFalse();

        // Permission cache now holds false for ADMIN, and snapshot is still unchanged
        assertThat(permCache.get(communityA.toString() + ":" + userId + ":ADMIN")).isNotNull();
        assertThat(membershipCacheService.find(communityA, userId)).isNotNull();
    }

    @Test
    @DisplayName("8. Concurrent permission checks do not cause race conditions or corrupt cache")
    void concurrentPermissionChecks_areThreadSafe() throws InterruptedException {
        Membership member = Membership.builder()
                .community(Community.builder().id(communityA).build())
                .userId(userId)
                .role(MembershipRole.MEMBER)
                .status(MembershipStatus.ACTIVE)
                .build();
        when(membershipRepository.findByCommunityIdAndUserId(communityA, userId))
                .thenReturn(Optional.of(member));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean anyFailed = new AtomicBoolean(false);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    boolean memberCheck = permissionEvaluator.hasCommunityRole(communityA, userId, "MEMBER");
                    boolean adminCheck = permissionEvaluator.hasCommunityRole(communityA, userId, "ADMIN");
                    if (!memberCheck || adminCheck) {
                        anyFailed.set(true);
                    }
                } catch (Exception ex) {
                    anyFailed.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(anyFailed.get()).isFalse();
    }
}
