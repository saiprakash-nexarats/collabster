package com.collabsphere_first.notification.worker;

import com.collabsphere_first.notification.config.NotificationProperties;
import com.collabsphere_first.notification.repository.NotificationDeliveryRepository;
import com.collabsphere_first.notification.repository.NotificationOutboxRepository;
import com.collabsphere_first.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetentionWorker {

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationProperties properties;

    // Run once daily at 3:00 AM
    @Scheduled(cron = "${collobster.notifications.retention.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            Instant now = Instant.now();
            Instant readCutoff = now.minus(Duration.ofDays(properties.getRetention().getReadRetentionDays()));
            Instant archivedCutoff = now.minus(Duration.ofDays(properties.getRetention().getArchivedRetentionDays()));
            Instant outboxCutoff = now.minus(Duration.ofDays(properties.getRetention().getOutboxRetentionDays()));

            int archivedDeleted = notificationRepository.deleteArchivedOlderThan(archivedCutoff);
            int readDeleted = notificationRepository.deleteReadOlderThan(readCutoff);
            int outboxDeleted = outboxRepository.deleteCompletedOlderThan(outboxCutoff);
            int deliveriesDeleted = deliveryRepository.deleteOlderThan(outboxCutoff);

            log.info("Notification retention cleanup executed: archivedDeleted={}, readDeleted={}, outboxDeleted={}, deliveriesDeleted={}",
                    archivedDeleted, readDeleted, outboxDeleted, deliveriesDeleted);
        } catch (Exception ex) {
            log.error("Failed during notification retention cleanup: {}", ex.getMessage(), ex);
        }
    }
}
