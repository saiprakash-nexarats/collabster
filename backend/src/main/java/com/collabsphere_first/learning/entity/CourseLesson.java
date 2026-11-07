package com.collabsphere_first.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_lessons", indexes = {
        @Index(name = "idx_course_lessons_course", columnList = "course_id"),
        @Index(name = "idx_course_lessons_order", columnList = "course_id, order_index")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "exercise_prompt", columnDefinition = "TEXT")
    private String exercisePrompt;

    @Column(name = "starter_code", columnDefinition = "TEXT")
    private String starterCode;

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 15;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

