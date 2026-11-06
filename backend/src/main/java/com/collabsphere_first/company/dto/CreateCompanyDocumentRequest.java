package com.collabsphere_first.company.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanyDocumentRequest {
    @NotNull(message = "Storage object ID is required")
    private UUID objectId;
    private String title;
    private String collection;
    private String accessScope;
}
