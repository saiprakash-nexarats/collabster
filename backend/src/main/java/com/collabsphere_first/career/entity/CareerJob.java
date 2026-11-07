package com.collabsphere_first.career.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_jobs", indexes = {
        @Index(name = "idx_career_jobs_employer", columnList = "employer_id"),
        @Index(name = "idx_career_jobs_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareerJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, length = 128)
    @Builder.Default
    private String location = "Flexible";

    @Column(name = "work_mode", nullable = false, length = 32)
    @Builder.Default
    private String workMode = "Flexible";

    @Column(name = "employment_type", nullable = false, length = 32)
    @Builder.Default
    private String employmentType = "Full-time";

    @Column(name = "salary_range", length = 128)
    private String salaryRange;

    @Column(name = "state", nullable = false, length = 32)
    @Builder.Default
    private String state = "published";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
