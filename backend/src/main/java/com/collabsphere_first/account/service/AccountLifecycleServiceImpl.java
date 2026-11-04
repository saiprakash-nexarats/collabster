package com.collabsphere_first.account.service;

import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.report.service.PlatformAuditLogService;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountLifecycleServiceImpl implements AccountLifecycleService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationDeviceService notificationDeviceService;
    private final MessagingPresenceService presenceService;
    private final PlatformAuditLogService auditLogService;
    private final com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService erasureOutboxService;
    private final com.collabsphere_first.security.service.AccountRevocationCoordinator accountRevocationCoordinator;

    @Override
    @Transactional
    public void deactivateAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for deactivation");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setDeactivated(true);
        user.setDeactivatedAt(LocalDateTime.now());
        user.setAccountEnabled(false);
        userRepository.save(user);

        // Invalidate active refresh tokens
        refreshTokenRepository.deleteByUser(user);

        // Revoke active push device tokens
        notificationDeviceService.revokeAllForUser(userId);

        // Evict presence
        presenceService.forceOffline(userId);

        // Revoke all realtime sessions and increment durable authVersion
        if (accountRevocationCoordinator != null) {
            accountRevocationCoordinator.revokeAccountSessions(userId, "Account deactivated");
        }

        auditLogService.logAction(userId, "ACCOUNT_DEACTIVATED", "USER", String.valueOf(userId), "User self-deactivated");
        log.info("User account {} deactivated successfully", userId);
    }

    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for deletion");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setAccountEnabled(false);

        // Anonymize personal PII
        user.setEmail("deleted_" + userId + "@deleted.collobster.invalid");
        user.setUsername("deleted_user_" + userId);
        user.setFirstName("Deleted");
        user.setLastName("User");
        user.setPassword(null);
        user.setProfilePicture(null);
        user.setBio(null);
        user.setCity(null);
        user.setCountry(null);
        user.setDateOfBirth(null);
        user.setLinkedinUrl(null);
        user.setPortfolioUrl(null);
        user.setYearsOfExperience(null);

        userRepository.save(user);

        // Invalidate refresh tokens
        refreshTokenRepository.deleteByUser(user);

        // Revoke active push device tokens
        notificationDeviceService.revokeAllForUser(userId);

        // Evict presence
        presenceService.forceOffline(userId);

        // Revoke all realtime sessions and increment durable authVersion
        if (accountRevocationCoordinator != null) {
            accountRevocationCoordinator.revokeAccountSessions(userId, "Account deleted");
        }

        // Enqueue durable downstream recommendation data erasure job
        erasureOutboxService.enqueueErasureJob(userId, "RECOMMENDATION", "ERASE");

        auditLogService.logAction(userId, "ACCOUNT_DELETED", "USER", String.valueOf(userId), "User self-deleted account; PII anonymized");
        log.info("User account {} deleted and anonymized successfully", userId);
    }

    @Override
    @Transactional
    public void suspendAccount(Long adminId, Long targetUserId, String reason) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminId));

        boolean isPlatformAdmin = admin.getRoles() != null && admin.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!isPlatformAdmin) {
            throw new IllegalStateException("Only platform administrators (ROLE_ADMIN) can suspend user accounts");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        target.setDeactivated(true);
        target.setAccountEnabled(false);
        target.setDeactivatedAt(LocalDateTime.now());
        userRepository.save(target);

        refreshTokenRepository.deleteByUser(target);
        notificationDeviceService.revokeAllForUser(targetUserId);

        // Evict presence
        presenceService.forceOffline(targetUserId);

        // Revoke all realtime sessions and increment durable authVersion
        if (accountRevocationCoordinator != null) {
            accountRevocationCoordinator.revokeAccountSessions(targetUserId, "Account suspended: " + reason);
        }

        auditLogService.logAction(adminId, "ACCOUNT_SUSPENDED", "USER", String.valueOf(targetUserId), "Reason: " + reason);
        log.info("User account {} suspended by admin {}", targetUserId, adminId);
    }

    @Override
    @Transactional
    public void reactivateAccount(Long adminId, Long targetUserId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found: " + adminId));

        boolean isPlatformAdmin = admin.getRoles() != null && admin.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);

        if (!isPlatformAdmin) {
            throw new IllegalStateException("Only platform administrators (ROLE_ADMIN) can reactivate user accounts");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        if (Boolean.TRUE.equals(target.getDeleted())) {
            throw new IllegalStateException("Deleted accounts cannot be reactivated.");
        }

        target.setDeactivated(false);
        target.setDeactivatedAt(null);
        target.setAccountEnabled(true);
        userRepository.save(target);

        auditLogService.logAction(adminId, "ACCOUNT_REACTIVATED", "USER", String.valueOf(targetUserId), "Reactivated by admin");
        log.info("User account {} reactivated by admin {}", targetUserId, adminId);
    }
}
