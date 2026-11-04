package com.collabsphere_first.notification.delivery.push;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.entity.Notification;
import com.collabsphere_first.notification.entity.NotificationDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockPushNotificationProvider implements PushNotificationProvider {

    private final NotificationProperties properties;

    // Test hook for simulating provider errors in tests
    private final AtomicReference<SimulatedBehavior> simulatedBehavior =
            new AtomicReference<>(SimulatedBehavior.NORMAL);

    public enum SimulatedBehavior {
        NORMAL,
        TIMEOUT,
        SERVER_ERROR,
        INVALID_TOKEN
    }

    public void setSimulatedBehavior(SimulatedBehavior behavior) {
        this.simulatedBehavior.set(behavior);
    }

    public void resetBehavior() {
        this.simulatedBehavior.set(SimulatedBehavior.NORMAL);
    }

    @Override
    public boolean isEnabled() {
        return properties.getPush().isEnabled();
    }

    @Override
    public PushSendResult send(NotificationDevice device, Notification notification) {
        if (!isEnabled()) {
            log.info("Push delivery skipped: push provider disabled in configuration (notificationId={})",
                    notification.getId());
            return PushSendResult.skipped("SKIPPED_DISABLED");
        }

        SimulatedBehavior behavior = simulatedBehavior.get();
        if (behavior == SimulatedBehavior.TIMEOUT) {
            log.warn("Push provider simulated timeout (notificationId={})", notification.getId());
            return PushSendResult.retryableFailure("GATEWAY_TIMEOUT", "Connection to push gateway timed out");
        } else if (behavior == SimulatedBehavior.SERVER_ERROR) {
            log.warn("Push provider simulated 5xx error (notificationId={})", notification.getId());
            return PushSendResult.retryableFailure("PUSH_SERVICE_UNAVAILABLE", "Push server returned 503");
        } else if (behavior == SimulatedBehavior.INVALID_TOKEN || (device.getToken() != null && device.getToken().contains("invalid"))) {
            log.warn("Push provider simulated unregistered token (notificationId={})", notification.getId());
            return PushSendResult.permanentFailure("UNREGISTERED", "Device token is no longer valid", true);
        }

        String providerMessageId = "mock-push-" + UUID.randomUUID();
        log.info("Delivered mock push notification (notificationId={}, category={}, platform={})",
                notification.getId(), notification.getCategory(), device.getPlatform());
        return PushSendResult.success(providerMessageId);
    }
}
