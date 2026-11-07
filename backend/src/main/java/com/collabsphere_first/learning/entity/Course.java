package com.collabsphere_first.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_courses_instructor", columnList = "instructor_id"),
        @Index(name = "idx_courses_state", columnList = "state"),
        @Index(name = "idx_courses_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "difficulty", nullable = false, length = 32)
    @Builder.Default
    private String difficulty = "beginner";

    @Column(name = "category", nullable = false, length = 64)
    @Builder.Default
    private String category = "Development";

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

