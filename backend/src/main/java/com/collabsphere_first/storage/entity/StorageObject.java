package com.collabsphere_first.storage.entity;

import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.enums.UploadState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "storage_objects", indexes = {
        @Index(name = "idx_storage_objects_owner", columnList = "owner_id"),
        @Index(name = "idx_storage_objects_workspace", columnList = "workspace_id"),
        @Index(name = "idx_storage_objects_state", columnList = "upload_state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageObject implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 32)
    @Builder.Default
    private StorageProvider storageProvider = StorageProvider.LOCAL;

    @Column(name = "storage_key", nullable = false, unique = true, length = 1024)
    private String storageKey;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "expected_bytes", nullable = false)
    private Long expectedBytes;

    @Column(name = "verified_bytes")
    private Long verifiedBytes;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_state", nullable = false, length = 32)
    @Builder.Default
    private UploadState uploadState = UploadState.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}

