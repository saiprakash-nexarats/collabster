package com.collabsphere_first.profile.mapper;

import com.collabsphere_first.profile.dto.response.ProfileResponse;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toProfileResponse(User user) {

        Role role = user.getRoles()
                .stream()
                .findFirst()
                .orElse(null);

        return ProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .country(user.getCountry())
                .city(user.getCity())
                .role(role != null ? role.getName() : null)
                .profileCompleted(user.getProfileCompleted())
                .build();
    }
}