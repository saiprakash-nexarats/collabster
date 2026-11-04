package com.collabsphere_first.post.entity;

import com.collabsphere_first.common.content.ContentRef;
import com.collabsphere_first.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Authoritative JPA aggregate root for a standard Collobster user post.
 * Distinct from CommunityPost (which requires a community relationship and community governance).
 */
@Entity
@Table(
        name = "platform_posts",
        indexes = {
                @Index(name = "idx_platform_posts_author_created", columnList = "author_id, created_at DESC"),
                @Index(name = "idx_platform_posts_feed_access", columnList = "status, visibility, created_at DESC"),
                @Index(name = "idx_platform_posts_author_cursor", columnList = "author_id, status, created_at DESC, id DESC")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PostStatus status = PostStatus.ACTIVE;

    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<PlatformPostMedia> media = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Constructs a lightweight polymorphic ContentRef for this platform post.
     */
    public ContentRef toContentRef() {
        return ContentRef.platformPost(this.id);
    }

    /**
     * Safely attaches a media item, maintaining bi-directional association.
     */
    public void addMedia(PlatformPostMedia mediaItem) {
        if (mediaItem != null) {
            mediaItem.setPost(this);
            this.media.add(mediaItem);
        }
    }

    /**
     * Clears all media attachments.
     */
    public void clearMedia() {
        this.media.forEach(m -> m.setPost(null));
        this.media.clear();
    }

    /**
     * Executes an authoritative soft deletion.
     */
    public void softDelete() {
        this.status = PostStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isDeleted() {
        return this.status == PostStatus.DELETED;
    }
}
