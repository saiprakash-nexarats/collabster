package com.collabsphere_first.recommendation.integration;

import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;
import com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus;
import com.collabsphere_first.account.erasure.repository.UserDataErasureJobRepository;
import com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService;
import com.collabsphere_first.account.erasure.worker.UserDataErasureWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate",
        "collobster.recommendation.feedback.base-url=http://127.0.0.1:8000",
        "collobster.recommendation.feedback.internal-token=${COLLOBSTER_RECOMMENDATION_INTERNAL_TOKEN:}"
})
@DisplayName("Live Python Recommendation Privacy Erasure Integration Test")
public class LivePythonPrivacyErasureIntegrationTest {

    @Autowired
    private UserDataErasureOutboxService outboxService;

    @Autowired
    private UserDataErasureJobRepository repository;

    @Autowired
    private UserDataErasureWorker worker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean isPythonAvailable() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 8000), 500);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    @DisplayName("Real FastAPI downstream privacy erasure: deletes user recommendation records and completes outbox job")
    void realFastApi_erasesUserEventsAndCompletesOutboxJob() {
        assumeTrue(isPythonAvailable(), "Python recommendation service on 127.0.0.1:8000 is not reachable");

        Long testUserId = 800000L + (long) (Math.random() * 100000);

        // 1. Seed recommendation_events directly into PostgreSQL for testUserId
        jdbcTemplate.update(
                "INSERT INTO recommendation_events (event_id, contract_version, user_id, event_type, content_type, content_id, occurred_at, ingested_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING",
                UUID.randomUUID().toString(),
                "1",
                testUserId,
                "VIEW",
                "PLATFORM_POST",
                UUID.randomUUID().toString(),
                java.sql.Timestamp.from(Instant.now()),
                java.sql.Timestamp.from(Instant.now())
        );

        Integer initialCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE user_id = ?",
                Integer.class,
                testUserId
        );
        assertThat(initialCount).isGreaterThanOrEqualTo(1);

        // 2. Enqueue erasure job
        UserDataErasureJob job = outboxService.enqueueErasureJob(testUserId, "RECOMMENDATION", "ERASE");
        assertThat(job).isNotNull();

        // 3. Worker claims and dispatches to live Python endpoint
        worker.processErasureJobs();

        // 4. Verify outbox job is COMPLETED
        UserDataErasureJob completedJob = repository.findById(job.getId()).orElseThrow();
        assertThat(completedJob.getStatus()).isEqualTo(UserDataErasureJobStatus.COMPLETED);
        assertThat(completedJob.getCompletedAt()).isNotNull();

        // 5. Verify records in recommendation_events table are 0
        Integer remainingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_events WHERE user_id = ?",
                Integer.class,
                testUserId
        );
        assertThat(remainingCount).isEqualTo(0);

        // 6. Phase 10.3: Verify durable privacy tombstone was registered in PostgreSQL recommendation_privacy_erasure table
        Integer tombstoneCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM recommendation_privacy_erasure WHERE user_id = ?",
                Integer.class,
                testUserId
        );
        assertThat(tombstoneCount).isEqualTo(1);
    }
}
