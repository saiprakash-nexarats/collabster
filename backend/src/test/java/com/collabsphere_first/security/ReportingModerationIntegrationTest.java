package com.collabsphere_first.security;

import com.collabsphere_first.messaging.repository.DirectMessageRepository;
import com.collabsphere_first.post.repository.PlatformPostRepository;
import com.collabsphere_first.report.dto.CreatePlatformReportRequest;
import com.collabsphere_first.report.dto.PlatformReportResponse;
import com.collabsphere_first.report.dto.ResolvePlatformReportRequest;
import com.collabsphere_first.report.entity.PlatformReport;
import com.collabsphere_first.report.enums.PlatformModerationAction;
import com.collabsphere_first.report.enums.PlatformReportReason;
import com.collabsphere_first.report.enums.PlatformReportStatus;
import com.collabsphere_first.report.enums.PlatformReportTargetType;
import com.collabsphere_first.report.exception.ReportAccessDeniedException;
import com.collabsphere_first.report.exception.ReportConflictException;
import com.collabsphere_first.report.exception.ReportNotFoundException;
import com.collabsphere_first.report.repository.PlatformReportRepository;
import com.collabsphere_first.report.service.PlatformAuditLogService;
import com.collabsphere_first.report.service.PlatformReportServiceImpl;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Platform Reporting & Moderation Hardening Tests")
public class ReportingModerationIntegrationTest {

    @Mock
    private PlatformReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformPostRepository postRepository;

    @Mock
    private DirectMessageRepository messageRepository;

    @Mock
    private PlatformAuditLogService auditLogService;

    @Mock
    private com.collabsphere_first.common.ratelimit.PlatformRateLimiter rateLimiter;

    @InjectMocks
    private PlatformReportServiceImpl reportService;

    private User reporter;
    private User reportedUser;
    private User platformAdmin;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1L).name(RoleName.ROLE_STUDENT).build();
        Role adminRole = Role.builder().id(99L).name(RoleName.ROLE_ADMIN).build();

        reporter = User.builder()
                .id(101L)
                .username("reporter_user")
                .roles(Set.of(studentRole))
                .build();

        reportedUser = User.builder()
                .id(202L)
                .username("bad_actor")
                .roles(Set.of(studentRole))
                .build();

        platformAdmin = User.builder()
                .id(999L)
                .username("admin_moderator")
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    @DisplayName("User can file a platform report on a user target successfully")
    void testFileReportSuccess() {
        CreatePlatformReportRequest request = CreatePlatformReportRequest.builder()
                .targetType(PlatformReportTargetType.USER)
                .targetId(String.valueOf(reportedUser.getId()))
                .reason(PlatformReportReason.HARASSMENT)
                .details("Harassing direct messages and comments")
                .build();

        when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));
        when(reportRepository.findByReporterUserIdAndTargetTypeAndTargetIdAndStatus(
                101L, PlatformReportTargetType.USER, "202", PlatformReportStatus.PENDING
        )).thenReturn(Optional.empty());

        PlatformReport savedReport = PlatformReport.builder()
                .id(UUID.randomUUID())
                .reporterUserId(101L)
                .targetType(PlatformReportTargetType.USER)
                .targetId("202")
                .reason(PlatformReportReason.HARASSMENT)
                .details("Harassing direct messages and comments")
                .status(PlatformReportStatus.PENDING)
                .build();

        when(reportRepository.save(any(PlatformReport.class))).thenReturn(savedReport);

        PlatformReportResponse response = reportService.fileReport(request, reporter.getId());

        assertNotNull(response);
        assertEquals(PlatformReportStatus.PENDING, response.getStatus());
        assertEquals("202", response.getTargetId());
        assertEquals(PlatformReportTargetType.USER, response.getTargetType());
        verify(reportRepository).save(any(PlatformReport.class));
    }

    @Test
    @DisplayName("Reporting a non-existent target throws ReportNotFoundException")
    void testFileReportTargetNotFound() {
        CreatePlatformReportRequest request = CreatePlatformReportRequest.builder()
                .targetType(PlatformReportTargetType.USER)
                .targetId("999999")
                .reason(PlatformReportReason.SPAM)
                .build();

        when(userRepository.findById(999999L)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.fileReport(request, reporter.getId()));
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate pending report by the same reporter throws ReportConflictException")
    void testDuplicateReportBlocked() {
        CreatePlatformReportRequest request = CreatePlatformReportRequest.builder()
                .targetType(PlatformReportTargetType.USER)
                .targetId(String.valueOf(reportedUser.getId()))
                .reason(PlatformReportReason.HARASSMENT)
                .build();

        when(userRepository.findById(reportedUser.getId())).thenReturn(Optional.of(reportedUser));

        PlatformReport existing = PlatformReport.builder()
                .id(UUID.randomUUID())
                .reporterUserId(101L)
                .targetType(PlatformReportTargetType.USER)
                .targetId("202")
                .status(PlatformReportStatus.PENDING)
                .build();

        when(reportRepository.findByReporterUserIdAndTargetTypeAndTargetIdAndStatus(
                101L, PlatformReportTargetType.USER, "202", PlatformReportStatus.PENDING
        )).thenReturn(Optional.of(existing));

        assertThrows(ReportConflictException.class, () -> reportService.fileReport(request, reporter.getId()));
        verify(reportRepository, never()).save(any());
    }

    @Test
    @DisplayName("Platform Admin can resolve report and resolution logs audit event")
    void testAdminResolveReport() {
        UUID reportId = UUID.randomUUID();
        PlatformReport pendingReport = PlatformReport.builder()
                .id(reportId)
                .reporterUserId(101L)
                .targetType(PlatformReportTargetType.USER)
                .targetId("202")
                .reason(PlatformReportReason.HARASSMENT)
                .status(PlatformReportStatus.PENDING)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(pendingReport));
        when(userRepository.findById(platformAdmin.getId())).thenReturn(Optional.of(platformAdmin));
        when(reportRepository.save(any(PlatformReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolvePlatformReportRequest resolveRequest = ResolvePlatformReportRequest.builder()
                .action(PlatformModerationAction.SUSPEND_ACCOUNT)
                .resolutionNotes("User account suspended due to confirmed harassment")
                .build();

        PlatformReportResponse response = reportService.resolveReport(reportId, resolveRequest, platformAdmin.getId());

        assertNotNull(response);
        assertEquals(PlatformReportStatus.RESOLVED, response.getStatus());
        assertEquals(PlatformModerationAction.SUSPEND_ACCOUNT, response.getResolution());
        assertEquals("User account suspended due to confirmed harassment", response.getResolutionNotes());
        verify(auditLogService).logAction(
                eq(platformAdmin.getId()),
                eq("RESOLVE_REPORT_SUSPEND_ACCOUNT"),
                eq("USER"),
                eq("202"),
                contains("User account suspended due to confirmed harassment")
        );
    }

    @Test
    @DisplayName("Non-admin user cannot resolve report (ReportAccessDeniedException)")
    void testNonAdminCannotResolveReport() {
        UUID reportId = UUID.randomUUID();
        PlatformReport pendingReport = PlatformReport.builder()
                .id(reportId)
                .reporterUserId(101L)
                .targetType(PlatformReportTargetType.USER)
                .targetId("202")
                .status(PlatformReportStatus.PENDING)
                .build();

        when(reportRepository.findById(reportId)).thenReturn(Optional.of(pendingReport));
        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));

        ResolvePlatformReportRequest resolveRequest = ResolvePlatformReportRequest.builder()
                .action(PlatformModerationAction.DISMISS_REPORT)
                .build();

        assertThrows(ReportAccessDeniedException.class, () ->
                reportService.resolveReport(reportId, resolveRequest, reporter.getId()));
    }
}
