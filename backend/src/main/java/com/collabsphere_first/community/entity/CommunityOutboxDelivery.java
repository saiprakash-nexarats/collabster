package com.collabsphere_first.community.entity;



import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "community_outbox_delivery")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityOutboxDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer_name", nullable = false, length = 150)
    private String consumerName;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public static CommunityOutboxDelivery pending(
            UUID eventId, String consumerName, Instant availableAt) {
        return CommunityOutboxDelivery.builder()
                .eventId(eventId)
                .consumerName(consumerName)
                .availableAt(availableAt)
                .attempts(0)
                .build();
    }
}
