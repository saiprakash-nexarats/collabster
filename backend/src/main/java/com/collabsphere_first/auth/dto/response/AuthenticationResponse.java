package com.collabsphere_first.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationResponse {

    private String accessToken;

    private String tokenType;

    private String refreshToken;

    private String email;

    private String displayName;

    private String username;

    private Long profileId;

    private Long userId;

}