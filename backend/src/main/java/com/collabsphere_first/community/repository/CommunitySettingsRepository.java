package com.collabsphere_first.community.repository;

import com.collabsphere_first.community.entity.CommunitySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommunitySettingsRepository extends JpaRepository<CommunitySettings, UUID> {}
