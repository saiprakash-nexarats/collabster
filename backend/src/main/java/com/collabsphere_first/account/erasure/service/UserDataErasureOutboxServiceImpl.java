package com.collabsphere_first.account.erasure.service;

import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;
import com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus;
import com.collabsphere_first.account.erasure.repository.UserDataErasureJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataErasureOutboxServiceImpl implements UserDataErasureOutboxService {

    private final UserDataErasureJobRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public UserDataErasureJob enqueueErasureJob(Long userId, String targetSystem, String operation) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        String sys = (targetSystem == null || targetSystem.isBlank()) ? "RECOMMENDATION" : targetSystem.trim().toUpperCase();
        String op = (operation == null || operation.isBlank()) ? "ERASE" : operation.trim().toUpperCase();

        Optional<UserDataErasureJob> existing = repository.findByUserIdAndTargetSystemAndOperation(userId, sys, op);
        if (existing.isPresent()) {
            UserDataErasureJob job = existing.get();
            if (job.getStatus() == UserDataErasureJobStatus.COMPLETED) {
                log.info("Erasure job for userId={}, system={}, op={} is already COMPLETED", userId, sys, op);
                return job;
            }
            if (job.getStatus() == UserDataErasureJobStatus.FAILED) {
                log.info("Resetting FAILED erasure job for retry: jobId={}", job.getId());
                job.setStatus(UserDataErasureJobStatus.PENDING);
                job.setAttemptCount(0);
                job.setNextAttemptAt(Instant.now());
                job.setLastErrorCode(null);
                job.setLastErrorMessage(null);
                return repository.save(job);
            }
            log.info("Erasure job for userId={}, system={}, op={} already exists with status={}", userId, sys, op, job.getStatus());
            return job;
        }

        UserDataErasureJob newJob = UserDataErasureJob.builder()
                .userId(userId)
                .targetSystem(sys)
                .operation(op)
                .status(UserDataErasureJobStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(Instant.now())
                .build();

        UserDataErasureJob saved = repository.save(newJob);
        log.info("Enqueued user data erasure job id={} for userId={}, targetSystem={}", saved.getId(), userId, sys);
        return saved;
    }

    @Override
    @Transactional
    public List<UserDataErasureJob> claimPendingJobs(String workerId, int limit, Duration leaseDuration) {
        Instant now = Instant.now();
        List<UserDataErasureJob> claimable = repository.findClaimableJobs(now, PageRequest.of(0, limit));
        List<UserDataErasureJob> claimed = new ArrayList<>();

        for (UserDataErasureJob job : claimable) {
            job.setStatus(UserDataErasureJobStatus.IN_PROGRESS);
            job.setLockOwner(workerId);
            job.setLockedAt(now);
            claimed.add(repository.save(job));
        }

        if (!claimed.isEmpty()) {
            log.info("Worker {} claimed {} user data erasure jobs", workerId, claimed.size());
        }
        return claimed;
    }

    @Override
    @Transactional
    public void recoverStaleLocks(Duration lockTimeout) {
        Instant staleThreshold = Instant.now().minus(lockTimeout);
        List<UserDataErasureJob> staleJobs = repository.findStaleJobs(staleThreshold);
        for (UserDataErasureJob job : staleJobs) {
            log.warn("Recovering stale erasure job {} locked at {} by {}", job.getId(), job.getLockedAt(), job.getLockOwner());
            job.setStatus(UserDataErasureJobStatus.RETRY);
            job.setLockOwner(null);
            job.setLockedAt(null);
            job.setNextAttemptAt(Instant.now());
            repository.save(job);
        }
    }

    @Override
    @Transactional
    public void markJobCompleted(UUID jobId, String workerId) {
        repository.findById(jobId).ifPresent(job -> {
            if (workerId != null && !workerId.equals(job.getLockOwner())) {
                log.warn("Worker mismatch on markJobCompleted: expected {} but was {}", job.getLockOwner(), workerId);
                return;
            }
            job.setStatus(UserDataErasureJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setLockOwner(null);
            job.setLockedAt(null);
            repository.save(job);
            log.info("User data erasure job {} marked COMPLETED", jobId);
        });
    }

    @Override
    @Transactional
    public void markJobRetry(UUID jobId, String workerId, String errorCode, String errorMessage, int maxAttempts) {
        repository.findById(jobId).ifPresent(job -> {
            if (workerId != null && !workerId.equals(job.getLockOwner())) {
                log.warn("Worker mismatch on markJobRetry: expected {} but was {}", job.getLockOwner(), workerId);
                return;
            }
            int newAttempts = job.getAttemptCount() + 1;
            job.setAttemptCount(newAttempts);
            job.setLastErrorCode(errorCode);
            job.setLastErrorMessage(errorMessage != null && errorMessage.length() > 950 ? errorMessage.substring(0, 950) : errorMessage);
            job.setLockOwner(null);
            job.setLockedAt(null);

            if (newAttempts >= maxAttempts) {
                job.setStatus(UserDataErasureJobStatus.FAILED);
                log.error("User data erasure job {} exceeded max attempts ({}) and marked FAILED", jobId, maxAttempts);
            } else {
                job.setStatus(UserDataErasureJobStatus.RETRY);
                long delaySeconds = (long) Math.min(300, Math.pow(2, newAttempts) * 2);
                job.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                log.warn("User data erasure job {} marked RETRY (attempt {}/{}, nextAttempt in {}s)",
                        jobId, newAttempts, maxAttempts, delaySeconds);
            }
            repository.save(job);
        });
    }
}
