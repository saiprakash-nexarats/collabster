package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.Membership;
import com.collabsphere_first.community.enums.MembershipRole;
import com.collabsphere_first.community.enums.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByCommunityIdAndUserId(UUID communityId, Long userId);
    boolean existsByCommunityIdAndUserId(UUID communityId, Long userId);
    boolean existsByCommunityIdAndUserIdAndStatus(UUID communityId, Long userId, MembershipStatus status);
    
    List<Membership> findByCommunityId(UUID communityId);
    Page<Membership> findByCommunityId(UUID communityId, Pageable pageable);
    Page<Membership> findByCommunityIdAndStatus(UUID communityId, MembershipStatus status, Pageable pageable);
    List<Membership> findByUserId(Long userId);
    Page<Membership> findByUserIdAndStatusIn(Long userId, List<MembershipStatus> statuses, Pageable pageable);
    
    long countByCommunityIdAndStatus(UUID communityId, MembershipStatus status);
    long countByCommunityIdAndRole(UUID communityId, MembershipRole role);
    long countByCommunityId(UUID communityId);
    long countByCommunityIdAndJoinedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    List<Membership> findByCommunityIdAndJoinedAtBetween(UUID communityId, java.time.Instant from, java.time.Instant to);
    List<Membership> findTop10ByCommunityIdOrderByContributionScoreDescUserIdAsc(UUID communityId);
    List<Membership> findByUserIdAndCommunityIdIn(Long userId, java.util.Collection<UUID> communityIds);
    List<Membership> findByCommunityIdAndStatus( UUID communityId, MembershipStatus status);
}
