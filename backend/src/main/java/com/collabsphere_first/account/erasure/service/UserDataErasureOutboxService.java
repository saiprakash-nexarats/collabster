package com.collabsphere_first.account.erasure.service;

import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface UserDataErasureOutboxService {

    UserDataErasureJob enqueueErasureJob(Long userId, String targetSystem, String operation);

    List<UserDataErasureJob> claimPendingJobs(String workerId, int limit, Duration leaseDuration);

    void recoverStaleLocks(Duration lockTimeout);

    void markJobCompleted(UUID jobId, String workerId);

    void markJobRetry(UUID jobId, String workerId, String errorCode, String errorMessage, int maxAttempts);
}
