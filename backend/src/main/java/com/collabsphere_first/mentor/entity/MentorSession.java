package com.collabsphere_first.mentor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_sessions", indexes = {
        @Index(name = "idx_mentor_sessions_mentor", columnList = "mentor_id"),
        @Index(name = "idx_mentor_sessions_learner", columnList = "learner_id"),
        @Index(name = "idx_mentor_sessions_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "learner_id", nullable = false)
    private Long learnerId;

    @Column(name = "slot_id")
    private UUID slotId;

    @Column(name = "session_type", nullable = false, length = 64)
    @Builder.Default
    private String sessionType = "1:1 mentoring";

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(name = "state", nullable = false, length = 32)
    @Builder.Default
    private String state = "requested";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
