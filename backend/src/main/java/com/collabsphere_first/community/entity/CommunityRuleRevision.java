package com.collabsphere_first.community.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_rule_revisions", uniqueConstraints = @UniqueConstraint(name = "uq_community_rule_revision", columnNames = {"rule_id", "version"}))
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommunityRuleRevision {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "rule_id", nullable = false)
    private CommunityRule rule;
    @Column(nullable = false) private int version;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @Column(nullable = false) private boolean active;
    @Column(name = "changed_by", nullable = false) private Long changedBy;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
