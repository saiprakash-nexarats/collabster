package com.collabsphere_first.services.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceReviewDto {
    private UUID id;
    private UUID serviceId;
    private UUID bookingId;
    private Long authorId;
    private String authorName;
    private Integer rating;
    private String reviewText;
    private Instant createdAt;
}

