package com.collabsphere_first.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "collobster.notifications")
public class NotificationProperties {

    private PushProperties push = new PushProperties();
    private EmailProperties email = new EmailProperties();
    private RealtimeProperties realtime = new RealtimeProperties();
    private OutboxProperties outbox = new OutboxProperties();
    private RetentionProperties retention = new RetentionProperties();

    @Data
    public static class PushProperties {
        private boolean enabled = false;
        private String provider = "FCM";
    }

    @Data
    public static class EmailProperties {
        private boolean enabled = true;
        private String fromAddress = "notifications@collobster.com";
    }

    @Data
    public static class RealtimeProperties {
        private boolean enabled = true;
    }

    @Data
    public static class OutboxProperties {
        private int batchSize = 50;
        private int lockLeaseSeconds = 60;
        private int maxAttempts = 5;
        private int baseBackoffSeconds = 10;
        private int workerIntervalMs = 2000;
    }

    @Data
    public static class RetentionProperties {
        private int readRetentionDays = 90;
        private int archivedRetentionDays = 30;
        private int outboxRetentionDays = 7;
    }
}
