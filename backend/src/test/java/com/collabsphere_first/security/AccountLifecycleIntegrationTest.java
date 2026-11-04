package com.collabsphere_first.security;

import com.collabsphere_first.account.service.AccountLifecycleServiceImpl;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.report.service.PlatformAuditLogService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Account Lifecycle (Deactivation, Deletion, Scrubbing, Suspension) Tests")
public class AccountLifecycleIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private NotificationDeviceService notificationDeviceService;

    @Mock
    private MessagingPresenceService presenceService;

    @Mock
    private PlatformAuditLogService auditLogService;

    @Mock
    private com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService erasureOutboxService;

    @Mock
    private com.collabsphere_first.security.service.AccountRevocationCoordinator accountRevocationCoordinator;

    @InjectMocks
    private AccountLifecycleServiceImpl accountLifecycleService;

    private User activeUser;
    private User platformAdmin;

    @BeforeEach
    void setUp() {
        Role studentRole = Role.builder().id(1L).name(RoleName.ROLE_STUDENT).build();
        Role adminRole = Role.builder().id(99L).name(RoleName.ROLE_ADMIN).build();

        activeUser = User.builder()
                .id(501L)
                .email("user501@collobster.com")
                .username("john_doe")
                .firstName("John")
                .lastName("Doe")
                .accountEnabled(true)
                .deactivated(false)
                .deleted(false)
                .roles(Set.of(studentRole))
                .build();

        platformAdmin = User.builder()
                .id(999L)
                .username("admin_user")
                .accountEnabled(true)
                .roles(Set.of(adminRole))
                .build();
    }

    @Test
    @DisplayName("Deactivate account disables account, purges tokens, evicts presence, and logs audit")
    void testDeactivateAccount() {
        when(userRepository.findById(501L)).thenReturn(Optional.of(activeUser));

        accountLifecycleService.deactivateAccount(501L);

        assertTrue(activeUser.getDeactivated());
        assertFalse(activeUser.getAccountEnabled());
        assertNotNull(activeUser.getDeactivatedAt());

        verify(userRepository).save(activeUser);
        verify(refreshTokenRepository).deleteByUser(activeUser);
        verify(notificationDeviceService).revokeAllForUser(501L);
        verify(presenceService).forceOffline(501L);
        verify(auditLogService).logAction(eq(501L), eq("ACCOUNT_DEACTIVATED"), eq("USER"), eq("501"), anyString());
    }

    @Test
    @DisplayName("Delete account scrubs PII, anonymizes username/email, purges tokens, and disables account")
    void testDeleteAccountScrubsPII() {
        when(userRepository.findById(501L)).thenReturn(Optional.of(activeUser));

        accountLifecycleService.deleteAccount(501L);

        assertTrue(activeUser.getDeleted());
        assertFalse(activeUser.getAccountEnabled());
        assertNotNull(activeUser.getDeletedAt());

        // Verify PII scrubbing
        assertEquals("deleted_501@deleted.collobster.invalid", activeUser.getEmail());
        assertEquals("deleted_user_501", activeUser.getUsername());
        assertEquals("Deleted", activeUser.getFirstName());
        assertEquals("User", activeUser.getLastName());
        assertNull(activeUser.getPassword());
        assertNull(activeUser.getProfilePicture());

        verify(userRepository).save(activeUser);
        verify(refreshTokenRepository).deleteByUser(activeUser);
        verify(notificationDeviceService).revokeAllForUser(501L);
        verify(presenceService).forceOffline(501L);
        verify(auditLogService).logAction(eq(501L), eq("ACCOUNT_DELETED"), eq("USER"), eq("501"), anyString());
    }

    @Test
    @DisplayName("Platform Admin can suspend user account with audit record")
    void testAdminSuspendAccount() {
        when(userRepository.findById(999L)).thenReturn(Optional.of(platformAdmin));
        when(userRepository.findById(501L)).thenReturn(Optional.of(activeUser));

        accountLifecycleService.suspendAccount(999L, 501L, "Severe Terms of Service violation");

        assertTrue(activeUser.getDeactivated());
        assertFalse(activeUser.getAccountEnabled());

        verify(userRepository).save(activeUser);
        verify(refreshTokenRepository).deleteByUser(activeUser);
        verify(notificationDeviceService).revokeAllForUser(501L);
        verify(presenceService).forceOffline(501L);
        verify(auditLogService).logAction(eq(999L), eq("ACCOUNT_SUSPENDED"), eq("USER"), eq("501"), contains("Terms of Service"));
    }

    @Test
    @DisplayName("Non-admin cannot suspend user account (IllegalStateException)")
    void testNonAdminCannotSuspendAccount() {
        when(userRepository.findById(501L)).thenReturn(Optional.of(activeUser));

        assertThrows(IllegalStateException.class, () ->
                accountLifecycleService.suspendAccount(501L, 501L, "Self-suspension attempt"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Platform Admin can reactivate deactivated account")
    void testAdminReactivateAccount() {
        activeUser.setDeactivated(true);
        activeUser.setAccountEnabled(false);

        when(userRepository.findById(999L)).thenReturn(Optional.of(platformAdmin));
        when(userRepository.findById(501L)).thenReturn(Optional.of(activeUser));

        accountLifecycleService.reactivateAccount(999L, 501L);

        assertFalse(activeUser.getDeactivated());
        assertTrue(activeUser.getAccountEnabled());
        assertNull(activeUser.getDeactivatedAt());

        verify(userRepository).save(activeUser);
        verify(auditLogService).logAction(eq(999L), eq("ACCOUNT_REACTIVATED"), eq("USER"), eq("501"), anyString());
    }
}
