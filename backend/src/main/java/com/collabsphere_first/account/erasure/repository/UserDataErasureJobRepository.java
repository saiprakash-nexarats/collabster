package com.collabsphere_first.account.erasure.repository;

import com.collabsphere_first.account.erasure.entity.UserDataErasureJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDataErasureJobRepository extends JpaRepository<UserDataErasureJob, UUID> {

    Optional<UserDataErasureJob> findByUserIdAndTargetSystemAndOperation(
            Long userId, String targetSystem, String operation
    );

    List<UserDataErasureJob> findByUserId(Long userId);

    @Query("SELECT j FROM UserDataErasureJob j WHERE (j.status = com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus.PENDING " +
           "OR (j.status = com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus.RETRY AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now))) " +
           "ORDER BY j.createdAt ASC")
    List<UserDataErasureJob> findClaimableJobs(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT j FROM UserDataErasureJob j WHERE j.status = com.collabsphere_first.account.erasure.entity.UserDataErasureJobStatus.IN_PROGRESS " +
           "AND j.lockedAt <= :staleThreshold")
    List<UserDataErasureJob> findStaleJobs(@Param("staleThreshold") Instant staleThreshold);
}
