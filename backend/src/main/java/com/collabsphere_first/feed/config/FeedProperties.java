package com.collabsphere_first.feed.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "collobster.feed")
public class FeedProperties {

    private int defaultPageSize = 20;
    private int maxPageSize = 50;
    private int maxCandidatePool = 500;
    private Duration sessionTtl = Duration.ofMinutes(20);
    private String sessionStore = "redis"; // "redis" or "in-memory"

    private Candidates candidates = new Candidates();
    private Ranking ranking = new Ranking();

    @Data
    public static class Candidates {
        private int selfLimit = 30;
        private int followingLimit = 200;
        private int connectionLimit = 200;
        private int communityLimit = 200;
        private int publicFallbackLimit = 100;
        private int minCandidateThreshold = 20;
    }

    @Data
    public static class Ranking {
        private double recencyWeight = 0.45;
        private double relationshipWeight = 0.40;
        private double engagementWeight = 0.15;
        private double recencyHalfLifeHours = 24.0;

        private double connectionSourceWeight = 1.0;
        private double followingSourceWeight = 0.85;
        private double communitySourceWeight = 0.70;
        private double selfSourceWeight = 0.50;
        private double fallbackSourceWeight = 0.20;
    }
}
