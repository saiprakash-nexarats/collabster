package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityInvitation;
import com.collabsphere_first.community.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface CommunityInvitationRepository extends JpaRepository<CommunityInvitation, UUID> {
    Optional<CommunityInvitation> findByTokenHash(String tokenHash);
    Page<CommunityInvitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status, Pageable pageable);
    Page<CommunityInvitation> findByCommunityIdAndStatus(UUID communityId, InvitationStatus status, Pageable pageable);
    boolean existsByCommunityIdAndInviteeIdAndStatus(UUID communityId, Long inviteeId, InvitationStatus status);

    @Modifying
    @Query("update CommunityInvitation i set i.status = :expired where i.status = :pending and i.expiresAt <= :now")
    int expirePendingBefore(@Param("now") Instant now, @Param("pending") InvitationStatus pending,
                            @Param("expired") InvitationStatus expired);
}
