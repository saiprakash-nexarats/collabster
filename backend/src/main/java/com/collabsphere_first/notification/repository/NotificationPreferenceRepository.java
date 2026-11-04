package com.collabsphere_first.notification.repository;

import com.collabsphere_first.notification.entity.NotificationPreference;
import com.collabsphere_first.notification.entity.NotificationPreferenceId;
import com.collabsphere_first.notification.enums.NotificationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

    Optional<NotificationPreference> findById_UserIdAndId_Category(Long userId, NotificationCategory category);

    List<NotificationPreference> findById_UserId(Long userId);
}
