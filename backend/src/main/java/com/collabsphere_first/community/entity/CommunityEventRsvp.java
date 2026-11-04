package com.collabsphere_first.community.entity;

import com.collabsphere_first.community.enums.EventRsvpStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "community_event_rsvps", uniqueConstraints = @UniqueConstraint(name = "uq_event_rsvp_user", columnNames = {"event_id", "user_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityEventRsvp {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id", nullable = false)
    private CommunityEvent event;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EventRsvpStatus status;
    @Column(name = "updated_at", nullable = false) @Builder.Default private Instant updatedAt = Instant.now();
}
