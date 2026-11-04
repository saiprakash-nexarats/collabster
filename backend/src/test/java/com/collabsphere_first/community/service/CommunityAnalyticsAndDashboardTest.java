package com.collabsphere_first.community.service;

import com.collabsphere_first.community.dto.CommunityDashboardDto;
import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.repository.*;
import com.collabsphere_first.community.security.CommunityRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityAnalyticsAndDashboardTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private CommunityChannelRepository channelRepository;
    @Mock private CommunityChannelMessageRepository messageRepository;
    @Mock private CommunityPollRepository pollRepository;
    @Mock private CommunityEventRepository eventRepository;
    @Mock private CommunityEventRsvpRepository eventRsvpRepository;
    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityRoleService roleService;

    @InjectMocks
    private CommunityDashboardService dashboardService;

    private UUID communityId;
    private Community community;
    private Long memberId = 301L;
    private Long modId = 302L;
    private Long adminId = 303L;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        community = Community.builder()
                .id(communityId)
                .name("Analytics Tech")
                .memberCount(25)
                .activityScore(java.math.BigDecimal.valueOf(77))
                .communityStatus(CommunityStatus.ACTIVE)
                .visibility(Visibility.PUBLIC)
                .build();
    }

    @Test
    @DisplayName("Regular MEMBER cannot access dashboard")
    void member_cannotAccessDashboard() {
        Membership member = Membership.builder().community(community).userId(memberId).role(MembershipRole.MEMBER).status(MembershipStatus.ACTIVE).build();
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, memberId)).thenReturn(Optional.of(member));
        when(roleService.isPlatformAdmin(memberId)).thenReturn(false);

        assertThatThrownBy(() -> dashboardService.dashboard(communityId, memberId))
                .isInstanceOf(InsufficientRoleException.class)
                .hasMessageContaining("Moderator access is required");
    }

    @Test
    @DisplayName("MODERATOR receives moderation view without full admin analytics")
    void moderator_receivesModerationViewOnly() {
        Membership mod = Membership.builder().community(community).userId(modId).role(MembershipRole.MODERATOR).status(MembershipStatus.ACTIVE).build();
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, modId)).thenReturn(Optional.of(mod));
        when(roleService.isPlatformAdmin(modId)).thenReturn(false);
        when(reportRepository.countByCommunityIdAndStatus(eq(communityId), any())).thenReturn(3L);
        when(postRepository.countByCommunityIdAndModerationStatus(eq(communityId), any())).thenReturn(2L);
        when(resourceRepository.countByCommunity_IdAndModerationStatus(eq(communityId), any())).thenReturn(1L);

        CommunityDashboardDto dto = dashboardService.dashboard(communityId, modId);

        assertThat(dto.isModerationView()).isTrue();
        assertThat(dto.getPendingReports()).isEqualTo(3L);
        assertThat(dto.getPendingModeration()).isEqualTo(3L);
        // Admin metrics must remain null
        assertThat(dto.getActiveMembers()).isNull();
        assertThat(dto.getMessageCount()).isNull();
    }

    @Test
    @DisplayName("ADMIN receives full dashboard and metrics")
    void admin_receivesFullDashboard() {
        Membership admin = Membership.builder().community(community).userId(adminId).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE).build();
        when(communityRepository.findById(communityId)).thenReturn(Optional.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(communityId, adminId)).thenReturn(Optional.of(admin));
        when(roleService.isPlatformAdmin(adminId)).thenReturn(false);
        when(membershipRepository.countByCommunityIdAndStatus(communityId, MembershipStatus.ACTIVE)).thenReturn(20L);
        when(postRepository.countByCommunityId(communityId)).thenReturn(50L);

        CommunityDashboardDto dto = dashboardService.dashboard(communityId, adminId);

        assertThat(dto.isModerationView()).isFalse();
        assertThat(dto.getActiveMembers()).isEqualTo(20L);
        assertThat(dto.getPostCount()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Analytics date range validation: end before start throws IllegalArgumentException")
    void analytics_invalidDateRange() {
        Membership admin = Membership.builder().community(community).userId(adminId).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE).build();
        when(membershipRepository.findByCommunityIdAndUserId(communityId, adminId)).thenReturn(Optional.of(admin));
        when(roleService.isPlatformAdmin(adminId)).thenReturn(false);

        Instant start = Instant.now();
        Instant end = start.minus(5, ChronoUnit.DAYS);

        assertThatThrownBy(() -> dashboardService.analytics(communityId, adminId, start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end time must be after start time");
    }

    @Test
    @DisplayName("Analytics date range validation: exceeds 366 days throws IllegalArgumentException")
    void analytics_exceedsMaxRange() {
        Membership admin = Membership.builder().community(community).userId(adminId).role(MembershipRole.ADMIN).status(MembershipStatus.ACTIVE).build();
        when(membershipRepository.findByCommunityIdAndUserId(communityId, adminId)).thenReturn(Optional.of(admin));
        when(roleService.isPlatformAdmin(adminId)).thenReturn(false);

        Instant start = Instant.now().minus(400, ChronoUnit.DAYS);
        Instant end = Instant.now();

        assertThatThrownBy(() -> dashboardService.analytics(communityId, adminId, start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceed 366 days");
    }
}
