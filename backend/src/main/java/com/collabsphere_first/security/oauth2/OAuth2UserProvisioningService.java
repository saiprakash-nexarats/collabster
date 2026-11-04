package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.auth.exception.RoleNotFoundException;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2UserProvisioningService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public User findOrCreateGoogleUser(
            String email,
            String firstName,
            String lastName,
            String profilePicture) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> createGoogleUser(
                        email,
                        firstName,
                        lastName,
                        profilePicture
                ));
    }

    private User createGoogleUser(
            String email,
            String firstName,
            String lastName,
            String profilePicture) {

        Role guestRole = roleRepository.findByName(RoleName.ROLE_GUEST)
                .orElseThrow(() ->
                        new RoleNotFoundException("ROLE_GUEST not found."));

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .profilePicture(profilePicture)
                .provider(AuthProvider.GOOGLE)
                .emailVerified(true)
                .accountEnabled(true)
                .build();

        user.getRoles().add(guestRole);

        return userRepository.save(user);
    }
}
