package com.collabsphere_first.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAllReadRequest {
    private String readUpToCursor;
    private Instant upTo;
}
