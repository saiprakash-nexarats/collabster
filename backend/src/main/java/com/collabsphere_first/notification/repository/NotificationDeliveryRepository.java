package com.collabsphere_first.notification.repository;

import com.collabsphere_first.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    List<NotificationDelivery> findByNotificationIdOrderBySentAtDesc(UUID notificationId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NotificationDelivery d WHERE d.sentAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
