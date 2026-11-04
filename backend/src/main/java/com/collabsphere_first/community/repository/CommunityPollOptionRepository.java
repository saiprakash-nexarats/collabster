package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityPollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunityPollOptionRepository extends JpaRepository<CommunityPollOption, UUID> {
    List<CommunityPollOption> findByPollIdOrderByPositionAsc(UUID pollId);
    List<CommunityPollOption> findByIdInAndPollId(List<UUID> ids, UUID pollId);
}
