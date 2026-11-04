package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunityChannelReadState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommunityChannelReadStateRepository extends JpaRepository<CommunityChannelReadState, UUID> {
    Optional<CommunityChannelReadState> findByChannelIdAndUserId(UUID channelId, Long userId);
}
