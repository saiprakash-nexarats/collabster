package com.collabsphere_first.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_enrollment", columnNames = {"course_id", "user_id"})
}, indexes = {
        @Index(name = "idx_course_enrollments_user", columnList = "user_id"),
        @Index(name = "idx_course_enrollments_course", columnList = "course_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "enrollment_state", nullable = false, length = 32)
    @Builder.Default
    private String enrollmentState = "active";

    @Column(name = "progress_percent", nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @CreationTimestamp
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
