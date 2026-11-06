package com.collabsphere_first.company.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "storage_object_id", nullable = false)
    private UUID storageObjectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 128)
    @Builder.Default
    private String collection = "General";

    @Column(name = "access_scope", nullable = false, length = 64)
    @Builder.Default
    private String accessScope = "company";

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
