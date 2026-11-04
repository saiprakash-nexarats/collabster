package com.collabsphere_first.account.erasure.worker;

import com.collabsphere_first.account.erasure.client.RecommendationPrivacyClient;
import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;
import com.collabsphere_first.account.erasure.service.UserDataErasureOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataErasureWorker {

    private final UserDataErasureOutboxService outboxService;
    private final RecommendationPrivacyClient privacyClient;

    private final String workerNodeId = "erasure-worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${collobster.account.erasure.worker-interval-ms:5000}")
    public void processErasureJobs() {
        try {
            outboxService.recoverStaleLocks(Duration.ofSeconds(60));

            List<UserDataErasureJob> jobs = outboxService.claimPendingJobs(workerNodeId, 10, Duration.ofSeconds(60));
            if (jobs.isEmpty()) {
                return;
            }

            for (UserDataErasureJob job : jobs) {
                processSingleJob(job);
            }
        } catch (Exception ex) {
            log.error("Unhandled error in UserDataErasureWorker: {}", ex.getMessage(), ex);
        }
    }

    public void processSingleJob(UserDataErasureJob job) {
        log.info("Processing user data erasure job {} for userId={}, target={}",
                job.getId(), job.getUserId(), job.getTargetSystem());

        try {
            if ("RECOMMENDATION".equalsIgnoreCase(job.getTargetSystem())) {
                boolean success = privacyClient.eraseUserData(job.getUserId());
                if (success) {
                    outboxService.markJobCompleted(job.getId(), workerNodeId);
                    log.info("Successfully completed user data erasure for userId={}", job.getUserId());
                    return;
                } else {
                    outboxService.markJobRetry(job.getId(), workerNodeId, "ERASURE_REJECTED", "Privacy client returned false", 5);
                    return;
                }
            } else {
                log.warn("Unknown target system {} for erasure job {}; marking completed", job.getTargetSystem(), job.getId());
                outboxService.markJobCompleted(job.getId(), workerNodeId);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch erasure for job {}: {}", job.getId(), ex.getMessage());
            outboxService.markJobRetry(job.getId(), workerNodeId, "DISPATCH_FAILED", ex.getMessage(), 5);
        }
    }
}
