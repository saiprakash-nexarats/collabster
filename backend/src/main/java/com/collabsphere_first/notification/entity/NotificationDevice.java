package com.collabsphere_first.notification.entity;

import com.collabsphere_first.notification.enums.DevicePlatform;
import com.collabsphere_first.notification.enums.DeviceProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private DevicePlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private DeviceProvider provider;

    @Column(name = "token", nullable = false, length = 1024)
    private String token;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public void revoke(Instant now) {
        this.enabled = false;
        this.revokedAt = now != null ? now : Instant.now();
        this.updatedAt = this.revokedAt;
    }

    public void reassignToUser(Long newUserId, Instant now) {
        this.userId = newUserId;
        this.enabled = true;
        this.revokedAt = null;
        this.updatedAt = now != null ? now : Instant.now();
        this.lastSeenAt = this.updatedAt;
    }

    public void touch(Instant now) {
        this.lastSeenAt = now != null ? now : Instant.now();
        this.updatedAt = this.lastSeenAt;
        if (!this.enabled && this.revokedAt != null) {
            this.enabled = true;
            this.revokedAt = null;
        }
    }
}
