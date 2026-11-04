package com.collabsphere_first.storage.dto;

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
public class UploadTicketResponse {
    private UUID id;
    private UUID objectId;
    private String uploadUrl;
    private String storageKey;
    private String filename;
    private String contentType;
    private Long expectedBytes;
    private Instant expiresAt;
    private String state;
}

