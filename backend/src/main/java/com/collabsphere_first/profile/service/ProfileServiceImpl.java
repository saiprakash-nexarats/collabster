package com.collabsphere_first.profile.service;

import com.collabsphere_first.profile.dto.request.CreateProfileRequest;
import com.collabsphere_first.profile.dto.response.ProfileResponse;
import com.collabsphere_first.profile.exception.ProfileAlreadyCompletedException;
import com.collabsphere_first.profile.exception.UsernameAlreadyExistsException;
import com.collabsphere_first.profile.mapper.ProfileMapper;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ProfileMapper profileMapper;

    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ProfileResponse createProfile(CreateProfileRequest request) {

        User user = currentUserService.getCurrentUser();

        if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            throw new ProfileAlreadyCompletedException();
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        if (request.getRole() == RoleName.ROLE_ADMIN) {
            throw new IllegalArgumentException("ROLE_ADMIN cannot be self-assigned via profile setup.");
        }

        Role selectedRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role selected."));

        user.getRoles().removeIf(role -> role.getName() == RoleName.ROLE_GUEST);

        user.getRoles().add(selectedRole);

        user.setUsername(request.getUsername());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setCountry(request.getCountry());
        user.setCity(request.getCity());


        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);

        return profileMapper.toProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentUserProfile() {

        User user = currentUserService.getCurrentUser();

        return profileMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(CreateProfileRequest request) {

        User user = currentUserService.getCurrentUser();

        if (request.getRole() == RoleName.ROLE_ADMIN) {
            throw new IllegalArgumentException("ROLE_ADMIN cannot be self-assigned via profile update.");
        }

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        Role selectedRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Invalid role selected."));

        user.getRoles().removeIf(role -> role.getName() != RoleName.ROLE_ADMIN);
        user.getRoles().add(selectedRole);

        user.setUsername(request.getUsername());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setCountry(request.getCountry());
        user.setCity(request.getCity());
        user.setProfileCompleted(true);

        User savedUser = userRepository.save(user);

        return profileMapper.toProfileResponse(savedUser);
    }

}
