package com.collabsphere_first.explore.trending;

import com.collabsphere_first.explore.config.ExploreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TrendingScoreCalculator {

    private final ExploreProperties exploreProperties;

    /**
     * Computes the deterministic trending score for a candidate content item
     * using interaction signals and exponential recency decay.
     *
     * @param likes count of likes in the trending window
     * @param saves count of bookmarks in the trending window
     * @param shares count of shares in the trending window
     * @param views count of deduplicated views in the trending window
     * @param watchTimeMs accumulated dwell/watch time in ms in the trending window
     * @param createdAt item creation timestamp
     * @param now current calculation instant
     * @return bounded deterministic trending score
     */
    public double calculateScore(
            long likes,
            long saves,
            long shares,
            long views,
            long watchTimeMs,
            Instant createdAt,
            Instant now
    ) {
        ExploreProperties.TrendingProperties props = exploreProperties.getTrending();

        double baseScore = (likes * props.getLikeWeight())
                + (saves * props.getSaveWeight())
                + (shares * props.getShareWeight())
                + (views * props.getViewWeight())
                + ((watchTimeMs / 1000.0) * props.getWatchTimeWeight());

        if (baseScore <= 0.0) {
            return 0.0;
        }

        if (createdAt == null) {
            return baseScore;
        }

        // Recency decay: score * decayFactor^(hoursOld / 6.0)
        long hoursOld = Math.max(0, Duration.between(createdAt, now != null ? now : Instant.now()).toHours());
        double decayFactor = props.getRecencyDecayFactor();
        double recencyMultiplier = Math.pow(decayFactor, hoursOld / 6.0);

        return baseScore * recencyMultiplier;
    }
}
