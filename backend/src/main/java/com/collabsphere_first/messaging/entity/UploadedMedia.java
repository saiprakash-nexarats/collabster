package com.collabsphere_first.messaging.entity;

import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "uploaded_media",
        indexes = {
                @Index(name = "idx_uploaded_media_owner", columnList = "owner_user_id"),
                @Index(name = "idx_uploaded_media_key", columnList = "storage_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "storage_key", nullable = false, length = 1024, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    private AttachmentMediaType mediaType;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "upload_status", nullable = false, length = 32)
    @Builder.Default
    private String uploadStatus = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
