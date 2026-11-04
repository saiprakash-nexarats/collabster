package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_notifications", indexes = @Index(name = "idx_community_notification_user_read", columnList = "user_id, read_at, created_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityNotification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "community_id") private UUID communityId;
    @Column(name = "notification_type", nullable = false, length = 50) private String type;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String body;
    @Column(name = "target_type", length = 50) private String targetType;
    @Column(name = "target_id", length = 100) private String targetId;
    @Column(name = "read_at") private Instant readAt;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
