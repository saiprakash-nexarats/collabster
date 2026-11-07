package com.collabsphere_first.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCertificateDto {
    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String recipientName;
    private String certificateNumber;
    private String verificationHash;
    private Instant issuedAt;
}

