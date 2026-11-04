package com.collabsphere_first.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "collabster.storage")
public class StorageProperties {

    private String provider = "local";
    private String uploadDir = "uploads";
    private String publicBaseUrl = "";
    private String signingSecret = "";
    private long maxFileSizeBytes = 26214400L; // 25 MB
    private int uploadExpirationMinutes = 15;

    private Set<String> allowedMimeTypes = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/csv",
            "text/markdown",
            "application/json",
            "application/zip",
            "audio/mpeg",
            "audio/wav",
            "video/mp4",
            "video/webm",
            "application/octet-stream"
    ));

    private S3Properties s3 = new S3Properties();

    @Data
    public static class S3Properties {
        private String endpoint = "";
        private String bucket = "";
        private String region = "us-east-1";
        private String accessKey = "";
        private String secretKey = "";
    }
}

