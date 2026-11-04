package com.collabsphere_first.notification;

import com.collabsphere_first.network.service.NetworkAccessService;
import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.delivery.email.EmailNotificationProviderImpl;
import com.collabsphere_first.notification.delivery.push.MockPushNotificationProvider;
import com.collabsphere_first.notification.delivery.push.PushSendResult;
import com.collabsphere_first.notification.dto.*;
import com.collabsphere_first.notification.entity.*;
import com.collabsphere_first.notification.enums.*;
import com.collabsphere_first.notification.exception.NotificationAccessDeniedException;
import com.collabsphere_first.notification.exception.NotificationCursorException;
import com.collabsphere_first.notification.repository.*;
import com.collabsphere_first.notification.service.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class NotificationCoreTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationDeviceRepository deviceRepository;

    @Mock
    private NotificationOutboxRepository outboxRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private NetworkAccessService networkAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JavaMailSender mailSender;

    private NotificationProperties properties;
    private NotificationPreferenceService preferenceService;
    private NotificationDeviceService deviceService;
    private NotificationOutboxService outboxService;
    private NotificationService notificationService;
    private MockPushNotificationProvider pushProvider;
    private EmailNotificationProviderImpl emailProvider;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.getPush().setEnabled(true);
        properties.getEmail().setEnabled(true);
        properties.getRealtime().setEnabled(true);
        properties.getOutbox().setMaxAttempts(5);
        properties.getOutbox().setBaseBackoffSeconds(10);
        properties.getOutbox().setLockLeaseSeconds(60);

        preferenceService = new NotificationPreferenceServiceImpl(preferenceRepository, properties);
        deviceService = new NotificationDeviceServiceImpl(deviceRepository);
        outboxService = new NotificationOutboxServiceImpl(outboxRepository, deliveryRepository, properties);
        pushProvider = new MockPushNotificationProvider(properties);
        emailProvider = new EmailNotificationProviderImpl(mailSender, properties);

        notificationService = new NotificationServiceImpl(
                notificationRepository,
                preferenceService,
                outboxService,
                networkAccessService,
                properties,
                eventPublisher
        );
    }

    // ==========================================
    // 1. NOTIFICATION STATE & MONOTONICITY
    // ==========================================

    @Test
    @DisplayName("Notification state transitions monotonically from UNSEEN -> SEEN -> READ -> ARCHIVED")
    void testNotificationStateTransitions() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUserId(100L)
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("EVENT-1")
                .createdAt(Instant.now())
                .build();
        log.info("Notification : " + notification.getId());
        assertThat(notification.getState()).isEqualTo(NotificationState.UNSEEN);

        // Mark seen
        boolean seenChanged = notification.markSeen(Instant.now());
        assertThat(seenChanged).isTrue();
        assertThat(notification.getState()).isEqualTo(NotificationState.SEEN);

        // Repeated mark seen is idempotent
        boolean seenAgain = notification.markSeen(Instant.now());
        assertThat(seenAgain).isFalse();
        assertThat(notification.getState()).isEqualTo(NotificationState.SEEN);

        // Mark read - read implies seen
        boolean readChanged = notification.markRead(Instant.now());
        assertThat(readChanged).isTrue();
        assertThat(notification.getState()).isEqualTo(NotificationState.READ);
        assertThat(notification.getSeenAt()).isNotNull();

        // Repeated mark read is idempotent
        boolean readAgain = notification.markRead(Instant.now());
        assertThat(readAgain).isFalse();
        assertThat(notification.getState()).isEqualTo(NotificationState.READ);

        // Archive
        boolean archived = notification.archive(Instant.now());
        assertThat(archived).isTrue();
        assertThat(notification.getState()).isEqualTo(NotificationState.ARCHIVED);
    }

    @Test
    @DisplayName("Direct markRead on UNSEEN notification automatically populates seenAt")
    void testDirectMarkReadImpliesSeen() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .recipientUserId(100L)
                .type(NotificationType.DIRECT_MESSAGE)
                .category(NotificationCategory.MESSAGING)
                .eventKey("EVENT-2")
                .createdAt(Instant.now())
                .build();

        assertThat(notification.getSeenAt()).isNull();
        assertThat(notification.getReadAt()).isNull();
        log.info("Notification " + notification.getId() );
        Instant now = Instant.now();
        notification.markRead(now);

        assertThat(notification.getReadAt()).isEqualTo(now);
        assertThat(notification.getSeenAt()).isEqualTo(now);
        assertThat(notification.getState()).isEqualTo(NotificationState.READ);
    }

    // ==========================================
    // 2. CURSOR PAGINATION LOGIC
    // ==========================================

    @Test
    @DisplayName("NotificationCursor properly encodes and decodes keyset cursor")
    void testCursorEncodingAndDecoding() {
        Instant now = Instant.ofEpochMilli(1710000000000L);
        UUID id = UUID.randomUUID();

        NotificationCursor cursor = new NotificationCursor(now, id);
        String cursorStr = cursor.toCursorString();

        assertThat(cursorStr).isNotBlank();
        NotificationCursor decoded = NotificationCursor.fromCursorString(cursorStr);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getCreatedAt()).isEqualTo(now);
        assertThat(decoded.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Malformed cursor strings throw NotificationCursorException")
    void testMalformedCursorRejection() {
        assertThat(NotificationCursor.fromCursorString(null)).isNull();
        assertThat(NotificationCursor.fromCursorString("   ")).isNull();

        assertThatThrownBy(() -> NotificationCursor.fromCursorString("not-a-valid-base-64!@#$"))
                .isInstanceOf(NotificationCursorException.class);

        // Valid base64 but invalid structure
        String invalidStructure = Base64.getUrlEncoder().withoutPadding().encodeToString("just-one-part".getBytes());
        assertThatThrownBy(() -> NotificationCursor.fromCursorString(invalidStructure))
                .isInstanceOf(NotificationCursorException.class);
    }

    // ==========================================
    // 3. CORE SERVICE INVARIANTS
    // ==========================================

    @Test
    @DisplayName("Self-notification is automatically suppressed")
    void testSelfNotificationSuppressed() {
        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(42L)
                .actorUserId(42L) // Same user
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("FOLLOW:42:42")
                .build();

        Notification result = notificationService.createNotification(cmd);

        assertThat(result).isNull();
        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("Notification is suppressed if network block exists between actor and recipient")
    void testNetworkBlockSuppressesNotification() {
        when(networkAccessService.isBlockedEitherDirection(10L, 20L)).thenReturn(true);

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(20L)
                .actorUserId(10L)
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("FOLLOW:10:20")
                .build();

        Notification result = notificationService.createNotification(cmd);

        assertThat(result).isNull();
        verify(networkAccessService).isBlockedEitherDirection(10L, 20L);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    @DisplayName("Event-key deduplication returns existing notification without duplicate insert")
    void testEventKeyDeduplication() {
        UUID existingId = UUID.randomUUID();
        Notification existing = Notification.builder()
                .id(existingId)
                .recipientUserId(20L)
                .actorUserId(10L)
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .eventKey("FOLLOW:10:20")
                .createdAt(Instant.now())
                .build();

        when(notificationRepository.findByEventKey("FOLLOW:10:20")).thenReturn(Optional.of(existing));

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(20L)
                .actorUserId(10L)
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("FOLLOW:10:20")
                .build();

        Notification result = notificationService.createNotification(cmd);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(existingId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("In-app preference suppression prevents notification persistence for non-security categories")
    void testInAppPreferenceSuppression() {
        // User disabled in-app for SOCIAL
        NotificationPreferenceId prefId = new NotificationPreferenceId(20L, NotificationCategory.SOCIAL);
        NotificationPreference pref = NotificationPreference.builder()
                .id(prefId)
                .inAppEnabled(false)
                .pushEnabled(true)
                .emailEnabled(false)
                .build();
        when(preferenceRepository.findById(prefId)).thenReturn(Optional.of(pref));

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(20L)
                .actorUserId(10L)
                .type(NotificationType.NEW_FOLLOW)
                .eventKey("FOLLOW:10:20")
                .build();

        Notification result = notificationService.createNotification(cmd);

        assertThat(result).isNull();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("SECURITY category in-app notifications cannot be suppressed by user preference")
    void testSecurityCategoryCannotBeSuppressed() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(UUID.randomUUID());
            return n;
        });

        CreateNotificationCommand cmd = CreateNotificationCommand.builder()
                .recipientUserId(20L)
                .type(NotificationType.SECURITY_ALERT)
                .eventKey("SECURITY:PASSWORD_RESET:20")
                .build();

        Notification result = notificationService.createNotification(cmd);

        assertThat(result).isNotNull();
        assertThat(result.getCategory()).isEqualTo(NotificationCategory.SECURITY);
        verify(notificationRepository).save(any(Notification.class));
    }

    // ==========================================
    // 4. DEVICE REGISTRATION & REASSIGNMENT
    // ==========================================

    @Test
    @DisplayName("Device registration hashes token and saves device")
    void testRegisterDevice() {
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .token("fcm-device-token-12345")
                .build();

        when(deviceRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        when(deviceRepository.save(any(NotificationDevice.class))).thenAnswer(inv -> {
            NotificationDevice d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        NotificationDeviceResponse response = deviceService.registerDevice(100L, request);

        assertThat(response).isNotNull();
        assertThat(response.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(response.getProvider()).isEqualTo(DeviceProvider.FCM);
        assertThat(response.isEnabled()).isTrue();

        ArgumentCaptor<NotificationDevice> captor = ArgumentCaptor.forClass(NotificationDevice.class);
        verify(deviceRepository).save(captor.capture());
        NotificationDevice saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getTokenHash()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo("fcm-device-token-12345"); // Must be hashed!
    }

    @Test
    @DisplayName("Token reassignment: registering same token under a different user reassigns ownership")
    void testDeviceReassignment() {
        NotificationDevice existingDevice = NotificationDevice.builder()
                .id(UUID.randomUUID())
                .userId(100L) // Originally belonged to user 100
                .platform(DevicePlatform.IOS)
                .provider(DeviceProvider.APNS)
                .token("shared-token")
                .tokenHash("hash-shared-token")
                .enabled(true)
                .build();

        when(deviceRepository.findByTokenHash(any())).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(NotificationDevice.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .platform(DevicePlatform.IOS)
                .provider(DeviceProvider.APNS)
                .token("shared-token")
                .build();

        // User 200 logs into the device
        NotificationDeviceResponse response = deviceService.registerDevice(200L, request);

        assertThat(response).isNotNull();
        assertThat(existingDevice.getUserId()).isEqualTo(200L); // Ownership transferred!
        assertThat(existingDevice.isEnabled()).isTrue();
        assertThat(existingDevice.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("Revoking device by an unauthorized user throws NotificationAccessDeniedException")
    void testRevokeDeviceIDORProtection() {
        UUID deviceId = UUID.randomUUID();
        NotificationDevice device = NotificationDevice.builder()
                .id(deviceId)
                .userId(100L) // Owned by 100
                .enabled(true)
                .build();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

        // User 200 attempts to revoke User 100's device
        assertThatThrownBy(() -> deviceService.revokeDevice(deviceId, 200L))
                .isInstanceOf(NotificationAccessDeniedException.class);
    }

    // ==========================================
    // 5. PREFERENCES & SECURITY
    // ==========================================

    @Test
    @DisplayName("Default preferences are applied when no database row exists")
    void testDefaultPreferences() {
        when(preferenceRepository.findById_UserId(100L)).thenReturn(Collections.emptyList());

        List<NotificationPreferenceDto> prefs = preferenceService.getPreferences(100L);

        assertThat(prefs).hasSize(NotificationCategory.values().length);
        for (NotificationPreferenceDto p : prefs) {
            assertThat(p.isInAppEnabled()).isTrue();
            assertThat(p.isPushEnabled()).isTrue();
            if (p.getCategory() == NotificationCategory.SECURITY) {
                assertThat(p.isEmailEnabled()).isTrue();
            } else {
                assertThat(p.isEmailEnabled()).isFalse();
            }
        }
    }

    @Test
    @DisplayName("Attempting to disable in-app notifications for SECURITY category is safely ignored")
    void testCannotDisableSecurityInAppPreference() {
        NotificationPreferenceId prefId = new NotificationPreferenceId(100L, NotificationCategory.SECURITY);
        when(preferenceRepository.findById(prefId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateNotificationPreferenceRequest update = UpdateNotificationPreferenceRequest.builder()
                .category(NotificationCategory.SECURITY)
                .inAppEnabled(false) // User attempts to disable in-app security alerts
                .build();

        NotificationPreferenceDto result = preferenceService.updatePreference(100L, update);

        assertThat(result.isInAppEnabled()).isTrue(); // Enforced TRUE!
    }

    // ==========================================
    // 6. PUSH & EMAIL PROVIDERS
    // ==========================================

    @Test
    @DisplayName("MockPushNotificationProvider handles simulated outcomes correctly")
    void testPushProviderBehaviors() {
        NotificationDevice device = NotificationDevice.builder()
                .id(UUID.randomUUID())
                .platform(DevicePlatform.ANDROID)
                .token("tok")
                .build();
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .build();

        // Normal success
        pushProvider.resetBehavior();
        PushSendResult normal = pushProvider.send(device, notification);
        assertThat(normal.isSuccess()).isTrue();
        assertThat(normal.getProviderMessageId()).isNotNull();

        // Timeout (retryable)
        pushProvider.setSimulatedBehavior(MockPushNotificationProvider.SimulatedBehavior.TIMEOUT);
        PushSendResult timeout = pushProvider.send(device, notification);
        assertThat(timeout.isSuccess()).isFalse();
        assertThat(timeout.isRetryable()).isTrue();

        // 5xx Server error (retryable)
        pushProvider.setSimulatedBehavior(MockPushNotificationProvider.SimulatedBehavior.SERVER_ERROR);
        PushSendResult serverError = pushProvider.send(device, notification);
        assertThat(serverError.isSuccess()).isFalse();
        assertThat(serverError.isRetryable()).isTrue();

        // Invalid token (permanent, must revoke)
        pushProvider.setSimulatedBehavior(MockPushNotificationProvider.SimulatedBehavior.INVALID_TOKEN);
        PushSendResult invalid = pushProvider.send(device, notification);
        assertThat(invalid.isSuccess()).isFalse();
        assertThat(invalid.isRetryable()).isFalse();
        assertThat(invalid.isShouldRevokeToken()).isTrue();

        // Disabled provider
        properties.getPush().setEnabled(false);
        PushSendResult disabled = pushProvider.send(device, notification);
        assertThat(disabled.isSkipped()).isTrue();
    }

    @Test
    @DisplayName("EmailNotificationProvider validates recipient email and delivers via JavaMailSender")
    void testEmailProvider() {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .type(NotificationType.NEW_FOLLOW)
                .category(NotificationCategory.SOCIAL)
                .build();

        emailProvider.sendNotificationEmail("user@example.com", "Alice", notification);
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        // Blank email throws IllegalArgumentException
        assertThatThrownBy(() -> emailProvider.sendNotificationEmail("", "Alice", notification))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("markAllRead with cursor decodes tuple and calls markAllReadUpToTuple")
    void testMarkAllReadWithCursor() {
        Instant cursorTime = Instant.ofEpochMilli(1710000000000L);
        UUID cursorId = UUID.randomUUID();
        NotificationCursor cursor = new NotificationCursor(cursorTime, cursorId);

        when(notificationRepository.markAllReadUpToTuple(eq(10L), eq(cursorTime), eq(cursorId), any(Instant.class)))
                .thenReturn(5);

        int updated = notificationService.markAllRead(10L, cursor.toCursorString(), null);

        assertThat(updated).isEqualTo(5);
        verify(notificationRepository).markAllReadUpToTuple(eq(10L), eq(cursorTime), eq(cursorId), any(Instant.class));
    }

    @Test
    @DisplayName("determineEligibleChannels suppresses all channels when conversation is muted")
    void testPreferenceMatrixConversationMuted() {
        Set<NotificationChannel> channels = preferenceService.determineEligibleChannels(10L, NotificationCategory.MESSAGING, true);
        assertThat(channels).isEmpty();

        Set<NotificationChannel> normalChannels = preferenceService.determineEligibleChannels(10L, NotificationCategory.MESSAGING, false);
        assertThat(normalChannels).contains(NotificationChannel.REALTIME, NotificationChannel.PUSH);
        assertThat(normalChannels).doesNotContain(NotificationChannel.EMAIL);
    }

    @Test
    @DisplayName("Stale worker is rejected when attempting to complete job owned by another worker")
    void testStaleWorkerRejected() {
        UUID outboxId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        NotificationOutbox job = NotificationOutbox.builder()
                .id(outboxId)
                .notificationId(notifId)
                .channel(NotificationChannel.PUSH)
                .status(OutboxStatus.IN_PROGRESS)
                .lockedBy("worker-B") // Owned by worker B
                .lockedAt(Instant.now())
                .build();

        when(outboxRepository.findById(outboxId)).thenReturn(Optional.of(job));

        // Worker A attempts stale completion
        outboxService.handleJobSuccess(outboxId, "worker-A", notifId, NotificationChannel.PUSH, "dev", "msg-1", 1);

        // Job status should NOT be updated to SENT, and no delivery should be saved by worker A
        assertThat(job.getStatus()).isEqualTo(OutboxStatus.IN_PROGRESS);
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Concurrent device registration insert conflict re-fetches and reassigns atomically")
    void testConcurrentDeviceRegistrationInsertRace() {
        String token = "race-token-123";
        String tokenHash = ((NotificationDeviceServiceImpl) deviceService).hashToken(token);

        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .build();

        NotificationDevice existingFromWinner = NotificationDevice.builder()
                .id(UUID.randomUUID())
                .userId(100L) // Originally registered to user 100
                .token(token)
                .tokenHash(tokenHash)
                .platform(DevicePlatform.ANDROID)
                .provider(DeviceProvider.FCM)
                .enabled(true)
                .build();

        // 1st findByTokenHash returns empty (loser sees nothing initially)
        // save throws DataIntegrityViolationException
        // 2nd findByTokenHash returns the row inserted by the winner
        when(deviceRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingFromWinner));

        when(deviceRepository.save(any(NotificationDevice.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key on token_hash"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDeviceResponse response = deviceService.registerDevice(200L, request);

        assertThat(response).isNotNull();
        assertThat(existingFromWinner.getUserId()).isEqualTo(200L); // Atomically reassigned to user 200
    }
}
