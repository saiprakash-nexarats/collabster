package com.collabsphere_first.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "collobster.messaging")
public class MessagingProperties {

    private Duration typingTtl = Duration.ofSeconds(6);
    private Duration presenceTtl = Duration.ofSeconds(60);
    private int maxMessageBodyLength = 10000;
    private int maxAttachmentsPerMessage = 10;
    private long maxAttachmentFileSize = 25 * 1024 * 1024L; // 25 MB
    private int defaultPageSize = 20;
    private int maxPageSize = 50;
    private Duration messageEditWindow = Duration.ofMinutes(15);

    private Set<String> allowedMimeTypes = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "audio/mpeg",
            "audio/ogg",
            "audio/mp4",
            "video/mp4",
            "video/webm",
            "video/quicktime",
            "application/pdf"
    );

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private int messagesPerMinute = 60;
        private int conversationsPerHour = 30;
        private int reactionsPerMinute = 60;
        private int typingPerMinute = 120;
    }
}
