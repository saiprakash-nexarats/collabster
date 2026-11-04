package com.collabsphere_first.community.service;

import com.collabsphere_first.community.entity.Community;
import com.collabsphere_first.community.enums.CommunityStatus;
import com.collabsphere_first.community.enums.Visibility;
import com.collabsphere_first.community.exception.InsufficientRoleException;
import com.collabsphere_first.community.repository.CommunityRepository;
import com.collabsphere_first.community.security.CommunityRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityDeletionAndRetentionTest {

    @Mock private CommunityRepository communityRepository;
    @Mock private CommunityRoleService roleService;
    @Mock private CommunityAuditLogService auditLogService;
    @Mock private CommunityEventOutboxService outboxService;

    @InjectMocks
    private CommunityLifecycleService lifecycleService;

    private UUID communityId;
    private Community community;
    private Long ownerId = 1L;
    private Long nonOwnerId = 2L;

    @BeforeEach
    void setUp() {
        communityId = UUID.randomUUID();
        community = Community.builder()
                .id(communityId)
                .name("Lifecycle Test Community")
                .ownerId(ownerId)
                .communityStatus(CommunityStatus.ARCHIVED)
                .archivedAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .visibility(Visibility.PUBLIC)
                .build();
    }

    @Test
    @DisplayName("Restore archived community succeeds for owner")
    void restore_succeedsForOwner() {
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.restore(communityId, ownerId);

        assertThat(community.getCommunityStatus()).isEqualTo(CommunityStatus.ACTIVE);
        assertThat(community.getArchivedAt()).isNull();
        verify(auditLogService).record(eq(community), eq(ownerId), eq("COMMUNITY_RESTORED"), eq("COMMUNITY"), eq(communityId.toString()), isNull());
        verify(outboxService).record(eq(communityId), eq("COMMUNITY_RESTORED"), eq(communityId.toString()), anyMap());
    }

    @Test
    @DisplayName("Restore fails when requested by non-owner")
    void restore_failsForNonOwner() {
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(roleService.isPlatformAdmin(nonOwnerId)).thenReturn(false);

        assertThatThrownBy(() -> lifecycleService.restore(communityId, nonOwnerId))
                .isInstanceOf(InsufficientRoleException.class);
    }

    @Test
    @DisplayName("Request deletion transitions community to DELETION_PENDING with 30-day lifecycle")
    void requestDeletion_setsStatusPending() {
        community.setCommunityStatus(CommunityStatus.ACTIVE);
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.requestDeletion(communityId, ownerId);

        assertThat(community.getCommunityStatus()).isEqualTo(CommunityStatus.DELETION_PENDING);
        assertThat(community.getDeletionRequestedAt()).isNotNull();
        verify(auditLogService).record(eq(community), eq(ownerId), eq("DELETION_SCHEDULED"), eq("COMMUNITY"), eq(communityId.toString()), eq("recoveryDays=30"));
        verify(outboxService).record(eq(communityId), eq("DELETION_SCHEDULED"), eq(communityId.toString()), anyMap());
    }

    @Test
    @DisplayName("Cancel deletion restores previous state")
    void cancelDeletion_restoresState() {
        community.setCommunityStatus(CommunityStatus.DELETION_PENDING);
        community.setDeletionRequestedAt(Instant.now());
        community.setArchivedAt(null);
        when(communityRepository.findByIdForUpdate(communityId)).thenReturn(Optional.of(community));
        when(communityRepository.save(any(Community.class))).thenAnswer(i -> i.getArgument(0));

        lifecycleService.cancelDeletion(communityId, ownerId);

        assertThat(community.getCommunityStatus()).isEqualTo(CommunityStatus.ACTIVE);
        assertThat(community.getDeletionRequestedAt()).isNull();
        verify(auditLogService).record(eq(community), eq(ownerId), eq("DELETION_CANCELLED"), eq("COMMUNITY"), eq(communityId.toString()), isNull());
    }

    @Test
    @DisplayName("Scheduled permanent deletion deletes communities older than 30 days and records audit")
    void scheduledDeletion_deletesExpiredCommunities() {
        community.setCommunityStatus(CommunityStatus.DELETION_PENDING);
        community.setDeletionRequestedAt(Instant.now().minus(31, ChronoUnit.DAYS));

        when(communityRepository.findByCommunityStatusAndDeletionRequestedAtBefore(eq(CommunityStatus.DELETION_PENDING), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(community)));

        lifecycleService.permanentlyDeleteExpiredCommunities();

        verify(auditLogService).record(eq(community), eq(ownerId), eq("COMMUNITY_DELETED"), eq("COMMUNITY"), eq(communityId.toString()), isNull());
        verify(outboxService).record(eq(communityId), eq("COMMUNITY_DELETED"), eq(communityId.toString()), anyMap());
        verify(communityRepository).delete(community);
    }
}
