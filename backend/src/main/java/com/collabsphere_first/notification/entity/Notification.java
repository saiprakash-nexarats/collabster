package com.collabsphere_first.notification.entity;

import com.collabsphere_first.notification.enums.NavigationTargetType;
import com.collabsphere_first.notification.enums.NotificationCategory;
import com.collabsphere_first.notification.enums.NotificationState;
import com.collabsphere_first.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 60)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private NotificationCategory category;

    @Column(name = "source_type", length = 60)
    private String sourceType;

    @Column(name = "source_id", length = 128)
    private String sourceId;

    @Column(name = "context_type", length = 60)
    private String contextType;

    @Column(name = "context_id", length = 128)
    private String contextId;

    @Column(name = "event_key", nullable = false, unique = true, length = 200)
    private String eventKey;

    @Column(name = "title_key", length = 120)
    private String titleKey;

    @Column(name = "body_key", length = 120)
    private String bodyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 60)
    private NavigationTargetType targetType;

    @Column(name = "target_id", length = 128)
    private String targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "seen_at")
    private Instant seenAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    public NotificationState getState() {
        if (archivedAt != null) {
            return NotificationState.ARCHIVED;
        }
        if (readAt != null) {
            return NotificationState.READ;
        }
        if (seenAt != null) {
            return NotificationState.SEEN;
        }
        return NotificationState.UNSEEN;
    }

    public boolean markSeen(Instant timestamp) {
        if (seenAt == null) {
            this.seenAt = timestamp != null ? timestamp : Instant.now();
            return true;
        }
        return false;
    }

    public boolean markRead(Instant timestamp) {
        Instant now = timestamp != null ? timestamp : Instant.now();
        boolean changed = false;
        if (this.readAt == null) {
            this.readAt = now;
            changed = true;
        }
        // Read implies seen
        if (this.seenAt == null) {
            this.seenAt = now;
            changed = true;
        }
        return changed;
    }

    public boolean archive(Instant timestamp) {
        if (this.archivedAt == null) {
            this.archivedAt = timestamp != null ? timestamp : Instant.now();
            return true;
        }
        return false;
    }
}
