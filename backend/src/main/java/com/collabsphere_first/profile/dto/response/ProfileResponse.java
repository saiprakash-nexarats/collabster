package com.collabsphere_first.profile.dto.response;

import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String username;

    private String profilePicture;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String country;

    private String city;

    private RoleName role;

    private Boolean profileCompleted;
}