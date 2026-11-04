package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "community_events", indexes = @Index(name = "idx_community_event_start", columnList = "community_id, starts_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "community_id", nullable = false)
    private Community community;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "starts_at", nullable = false) private Instant startsAt;
    @Column(name = "ends_at", nullable = false) private Instant endsAt;
    @Column(length = 500) private String location;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
