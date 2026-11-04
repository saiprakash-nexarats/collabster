package com.collabsphere_first.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.Resource;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageFileResource {
    private Resource resource;
    private String filename;
    private String contentType;
    private long contentLength;
}

