package com.collabsphere_first.mentor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mentor_profiles", indexes = {
        @Index(name = "idx_mentor_profiles_user", columnList = "user_id"),
        @Index(name = "idx_mentor_profiles_state", columnList = "state")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "headline", nullable = false)
    private String headline;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 8)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "timezone", nullable = false, length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "state", nullable = false, length = 32)
    @Builder.Default
    private String state = "active";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
