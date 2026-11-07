package com.collabsphere_first.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_completions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_lesson_completion", columnNames = {"lesson_id", "user_id"})
}, indexes = {
        @Index(name = "idx_lesson_completions_user", columnList = "user_id"),
        @Index(name = "idx_lesson_completions_lesson", columnList = "lesson_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt;
}

