package com.collabsphere_first.account;

import com.collabsphere_first.account.erasure.client.RecommendationPrivacyClient;
import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;
import com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus;
import com.collabsphere_first.account.erasure.repository.UserDataErasureJobRepository;
import com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService;
import com.collabsphere_first.account.erasure.worker.UserDataErasureWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5433/collabster",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@DisplayName("User Data Erasure Outbox & Worker Integration Tests")
class UserDataErasureOutboxTest {

    @Autowired
    private UserDataErasureOutboxService outboxService;

    @Autowired
    private UserDataErasureJobRepository repository;

    @MockitoBean
    private RecommendationPrivacyClient privacyClient;

    @Autowired
    private UserDataErasureWorker worker;

    @Test
    @DisplayName("Enqueue erasure job creates pending outbox record idempotently")
    void enqueueErasureJob_createsRecordIdempotently() {
        Long userId = 930000L + (long) (Math.random() * 10000);

        try {
            UserDataErasureJob job1 = outboxService.enqueueErasureJob(userId, "RECOMMENDATION", "ERASE");
            assertThat(job1).isNotNull();
            assertThat(job1.getUserId()).isEqualTo(userId);
            assertThat(job1.getStatus()).isEqualTo(UserDataErasureJobStatus.PENDING);

            // Calling enqueue again for the same user must be idempotent and return existing job
            UserDataErasureJob job2 = outboxService.enqueueErasureJob(userId, "RECOMMENDATION", "ERASE");
            assertThat(job2.getId()).isEqualTo(job1.getId());

            List<UserDataErasureJob> jobs = repository.findByUserId(userId);
            assertThat(jobs).hasSize(1);
        } finally {
            repository.deleteAll(repository.findByUserId(userId));
        }
    }

    @Test
    @DisplayName("Worker claims and completes erasure job when Python client succeeds")
    void worker_claimsAndCompletesErasureJob() {
        Long userId = 910000L + (long) (Math.random() * 10000);
        when(privacyClient.eraseUserData(eq(userId))).thenReturn(true);

        try {
            UserDataErasureJob job = outboxService.enqueueErasureJob(userId, "RECOMMENDATION", "ERASE");
            assertThat(job.getStatus()).isEqualTo(UserDataErasureJobStatus.PENDING);

            // Process jobs
            worker.processErasureJobs();

            UserDataErasureJob updated = repository.findById(job.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(UserDataErasureJobStatus.COMPLETED);
            assertThat(updated.getCompletedAt()).isNotNull();
            verify(privacyClient, atLeastOnce()).eraseUserData(eq(userId));
        } finally {
            repository.deleteAll(repository.findByUserId(userId));
        }
    }

    @Test
    @DisplayName("Worker retries erasure job when Python client fails")
    void worker_retriesErasureJob_whenClientFails() {
        Long userId = 920000L + (long) (Math.random() * 10000);
        when(privacyClient.eraseUserData(eq(userId))).thenThrow(new RuntimeException("Connection refused"));

        try {
            UserDataErasureJob job = outboxService.enqueueErasureJob(userId, "RECOMMENDATION", "ERASE");

            worker.processErasureJobs();

            UserDataErasureJob updated = repository.findById(job.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(UserDataErasureJobStatus.RETRY);
            assertThat(updated.getAttemptCount()).isGreaterThanOrEqualTo(1);
            assertThat(updated.getLastErrorCode()).isEqualTo("DISPATCH_FAILED");
            assertThat(updated.getNextAttemptAt()).isNotNull();
        } finally {
            repository.deleteAll(repository.findByUserId(userId));
        }
    }
}
