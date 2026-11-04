package com.collabsphere_first.explore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "collobster.explore")
public class ExploreProperties {

    private HomeProperties home = new HomeProperties();
    private SearchProperties search = new SearchProperties();
    private TrendingProperties trending = new TrendingProperties();
    private DiscoveryProperties discovery = new DiscoveryProperties();

    @Getter
    @Setter
    public static class HomeProperties {
        private int trendingPreviewLimit = 10;
        private int recentPreviewLimit = 10;
        private int peoplePreviewLimit = 10;
        private int communityPreviewLimit = 10;
    }

    @Getter
    @Setter
    public static class SearchProperties {
        private int minQueryLength = 2;
        private int maxQueryLength = 100;
        private int previewLimit = 5;
        private int defaultPageSize = 20;
        private int maxPageSize = 50;
    }

    @Getter
    @Setter
    public static class TrendingProperties {
        private Duration window = Duration.ofHours(24);
        private double likeWeight = 3.0;
        private double shareWeight = 5.0;
        private double viewWeight = 0.5;
        private double watchTimeWeight = 0.001; // per ms (~1.0 per second)
        private double saveWeight = 2.0;
        private double recencyDecayFactor = 0.95;
        private int candidateLimit = 200;
    }

    @Getter
    @Setter
    public static class DiscoveryProperties {
        private int defaultPageSize = 20;
        private int maxPageSize = 50;
        private int candidateLimit = 100;
        private boolean excludeFollowing = true;
        private boolean excludeConnected = true;
    }
}
