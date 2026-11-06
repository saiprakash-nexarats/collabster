package com.collabsphere_first.company.dto;

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
public class CompanyDocumentDto {
    private UUID id;
    private UUID companyId;
    private UUID objectId;
    private String title;
    private String filename;
    private String contentType;
    private Long bytes;
    private String collection;
    private String accessScope;
    private Long createdBy;
    private Instant createdAt;
}
