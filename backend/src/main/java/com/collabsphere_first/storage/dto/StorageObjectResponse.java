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
public class StorageObjectResponse {
    private UUID id;
    private UUID objectId;
    private String filename;
    private String contentType;
    private Long verifiedBytes;
    private Long expectedBytes;
    private String state;
    private String storageKey;
    private String downloadUrl;
    private String checksum;
    private Instant createdAt;
    private Instant completedAt;
}

