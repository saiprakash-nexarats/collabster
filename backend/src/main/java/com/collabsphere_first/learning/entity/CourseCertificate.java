package com.collabsphere_first.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_certificates", uniqueConstraints = {
        @UniqueConstraint(name = "uq_course_certificate", columnNames = {"course_id", "user_id"})
}, indexes = {
        @Index(name = "idx_course_certificates_user", columnList = "user_id"),
        @Index(name = "idx_course_certificates_number", columnList = "certificate_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 64)
    private String certificateNumber;

    @Column(name = "verification_hash", nullable = false, unique = true, length = 128)
    private String verificationHash;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;
}
