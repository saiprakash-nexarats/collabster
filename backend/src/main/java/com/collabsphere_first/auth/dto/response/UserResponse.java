package com.collabsphere_first.auth.dto.response;

import com.collabsphere_first.user.entity.Gender;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private Integer age;

    private Gender gender;

    private String profilePicture;

    private boolean emailVerified;

}