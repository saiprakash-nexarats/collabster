package com.collabsphere_first.community.entity;

import com.collabsphere_first.community.enums.ModerationStatus;
import com.collabsphere_first.community.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column(nullable = false)
    private String title;

    @Column(name = "storage_key_or_url", nullable = false, columnDefinition = "TEXT")
    private String storageKeyOrUrl;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String category;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.PENDING_REVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
