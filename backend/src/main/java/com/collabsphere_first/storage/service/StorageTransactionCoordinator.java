package com.collabsphere_first.storage.service;

import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.exception.StorageObjectNotFoundException;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Coordinates atomic, isolated database state transitions for storage upload sessions.
 * Each transition runs in an independent transaction (REQUIRES_NEW) to ensure that
 * failures, crashes, or exceptions thrown during long filesystem streaming never roll back
 * durable state updates (such as UPLOADING claims or FAILED records) back to PENDING.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageTransactionCoordinator {

    private final StorageObjectRepository repository;

    /**
     * Atomically claims a PENDING upload session, transitioning it to UPLOADING.
     * Committed in an isolated transaction so that subsequent streaming errors cannot
     * revert the session back to PENDING.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int claimSession(UUID objectId, Instant now) {
        int updated = repository.claimUploadSession(objectId, now);
        if (updated > 0) {
            repository.findByIdAndIsDeletedFalse(objectId).ifPresent(obj -> {
                obj.setUploadState(UploadState.UPLOADING);
            });
        }
        log.debug("Atomic claim result for object {}: {} row(s) updated to UPLOADING", objectId, updated);
        return updated;
    }

    /**
     * Durably marks an upload session as FAILED in its own committed transaction.
     * Guaranteed to persist even when unchecked exceptions are thrown to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID objectId, String failureReason) {
        repository.findByIdAndIsDeletedFalse(objectId).ifPresent(obj -> {
            obj.setUploadState(UploadState.FAILED);
            repository.save(obj);
            log.warn("Durably marked storage upload session {} as FAILED. Reason: {}", objectId, failureReason);
        });
    }

    /**
     * Records verified bytes and cryptographic checksum upon successful binary streaming.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordStreamSuccess(UUID objectId, long verifiedBytes, String checksum) {
        repository.findByIdAndIsDeletedFalse(objectId).ifPresent(obj -> {
            obj.setVerifiedBytes(verifiedBytes);
            obj.setChecksum(checksum);
            repository.save(obj);
            log.info("Recorded stream success for object {}: verifiedBytes={}, checksum={}",
                    objectId, verifiedBytes, checksum);
        });
    }

    /**
     * Durably completes an upload session, marking it COMPLETED.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StorageObject completeSession(UUID objectId, long verifiedBytes, String checksum, Instant now) {
        StorageObject obj = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        obj.setUploadState(UploadState.COMPLETED);
        obj.setVerifiedBytes(verifiedBytes);
        obj.setChecksum(checksum);
        obj.setCompletedAt(now);
        StorageObject saved = repository.save(obj);
        log.info("Successfully marked storage upload session {} as COMPLETED", objectId);
        return saved;
    }
}
