package com.collabsphere_first.mentor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_slots", indexes = {
        @Index(name = "idx_mentor_slots_mentor", columnList = "mentor_id"),
        @Index(name = "idx_mentor_slots_time", columnList = "slot_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "mentor_id", nullable = false)
    private Long mentorId;

    @Column(name = "slot_time", nullable = false)
    private Instant slotTime;

    @Column(name = "duration_minutes", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(name = "booked", nullable = false)
    @Builder.Default
    private Boolean booked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

