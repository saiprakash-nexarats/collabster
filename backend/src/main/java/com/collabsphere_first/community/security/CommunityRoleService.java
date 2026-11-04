package com.collabsphere_first.community.security;

import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Bridge service that checks if a userId belongs to a Platform Admin.
 * Wraps Madhuri's UserRepository to determine platform-level admin status
 * by checking the user's assigned roles (ROLE_ADMIN).
 */
@Service
@RequiredArgsConstructor
public class CommunityRoleService {

    private final UserRepository userRepository;

    /**
     * Returns true if the given userId has the ROLE_ADMIN platform role.
     * Platform Admins bypass all community-level permission checks.
     */
    public boolean isPlatformAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN))
                .orElse(false);
    }
}
