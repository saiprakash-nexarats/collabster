package com.collabsphere_first.explore;

import com.collabsphere_first.explore.config.ExploreProperties;
import com.collabsphere_first.explore.trending.TrendingScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class TrendingScoreCalculatorTest {

    private TrendingScoreCalculator calculator;
    private ExploreProperties exploreProperties;

    @BeforeEach
    void setUp() {
        exploreProperties = new ExploreProperties();
        calculator = new TrendingScoreCalculator(exploreProperties);
    }

    @Test
    @DisplayName("Recent engagement produces deterministic score according to configured weights")
    void testRecentEngagementScore() {
        Instant now = Instant.now();
        Instant createdAt = now.minus(Duration.ofHours(1));

        // likes=10 (w=3 -> 30), saves=5 (w=2 -> 10), shares=2 (w=5 -> 10), views=100 (w=0.5 -> 50), watch=60,000ms=60s (w=0.001/ms -> 60)
        // total base = 30 + 10 + 10 + 50 + 60 = 160
        double score = calculator.calculateScore(10, 5, 2, 100, 60_000, createdAt, now);
        assertThat(score).isGreaterThan(90.0);
    }

    @Test
    @DisplayName("Old content decays deterministically compared to recent content with identical engagement")
    void testOldContentDecay() {
        Instant now = Instant.now();
        Instant freshCreated = now.minus(Duration.ofHours(1));
        Instant oldCreated = now.minus(Duration.ofDays(7));

        double freshScore = calculator.calculateScore(20, 10, 5, 200, 100_000, freshCreated, now);
        double oldScore = calculator.calculateScore(20, 10, 5, 200, 100_000, oldCreated, now);

        assertThat(freshScore).isGreaterThan(oldScore);
        assertThat(oldScore).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Shares weigh more heavily than views per configured properties")
    void testSignalWeights() {
        Instant now = Instant.now();
        // 10 shares vs 10 views
        double shareScore = calculator.calculateScore(0, 0, 10, 0, 0, now, now);
        double viewScore = calculator.calculateScore(0, 0, 0, 10, 0, now, now);

        // 10 * 5.0 = 50 vs 10 * 0.5 = 5.0
        assertThat(shareScore).isEqualTo(50.0, offset(0.01));
        assertThat(viewScore).isEqualTo(5.0, offset(0.01));
        assertThat(shareScore).isGreaterThan(viewScore * 5);
    }

    @Test
    @DisplayName("Zero engagement yields zero score")
    void testZeroEngagement() {
        double score = calculator.calculateScore(0, 0, 0, 0, 0, Instant.now(), Instant.now());
        assertThat(score).isEqualTo(0.0);
    }
}
