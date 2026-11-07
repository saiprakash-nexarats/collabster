package com.collabsphere_first.career.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_applications",
        uniqueConstraints = @UniqueConstraint(name = "uq_job_applicant", columnNames = {"job_id", "applicant_id"}),
        indexes = {
                @Index(name = "idx_career_applications_job", columnList = "job_id"),
                @Index(name = "idx_career_applications_applicant", columnList = "applicant_id"),
                @Index(name = "idx_career_applications_state", columnList = "state")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "resume_object_id")
    private UUID resumeObjectId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "state", nullable = false, length = 32)
    @Builder.Default
    private String state = "submitted";

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

