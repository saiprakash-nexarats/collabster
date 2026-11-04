package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityRuleRepository extends JpaRepository<CommunityRule, UUID> {

    List<CommunityRule> findByCommunityIdAndActiveTrueOrderByPositionAsc(UUID communityId);

    @Query("""
        SELECT r
        FROM CommunityRule r
        WHERE r.id = :ruleId
          AND r.community.id = :communityId
        """)
    Optional<CommunityRule> findByRuleIdAndCommunityId(
            @Param("ruleId") UUID ruleId,
            @Param("communityId") UUID communityId
    );

    int countByCommunityId(UUID communityId);
}