package com.collabsphere_first.community.entity;

import com.collabsphere_first.community.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "communities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommunityType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommunityCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleState lifecycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "community_status", nullable = false, length = 30)
    @Builder.Default
    private CommunityStatus communityStatus = CommunityStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "requires_moderation", nullable = false)
    @Builder.Default
    private boolean requiresModeration = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;

    @Column(name = "member_count", nullable = false)
    @Builder.Default
    private int memberCount = 1;

    @Column(name = "activity_score", nullable = false, precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal activityScore = BigDecimal.ZERO;

    @Column(name = "linked_event_id")
    private UUID linkedEventId;

    @Column(name = "linked_course_id")
    private UUID linkedCourseId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;
}
