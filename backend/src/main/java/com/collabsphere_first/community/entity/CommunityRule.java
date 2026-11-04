package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_rules", indexes = @Index(name = "idx_community_rule_order", columnList = "community_id, active, position"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityRule {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "community_id", nullable = false)
    private Community community;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false) private int position;
    @Column(nullable = false) @Builder.Default private int version = 1;
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) @Builder.Default private Instant updatedAt = Instant.now();
}
