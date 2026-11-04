package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserProvisioningService oAuth2UserProvisioningService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oauth2User =
                new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oauth2User.getAttribute("email");

        User user = oAuth2UserProvisioningService.findOrCreateGoogleUser(
                email,
                oauth2User.getAttribute("given_name"),
                oauth2User.getAttribute("family_name"),
                oauth2User.getAttribute("picture")
        );

        return new AppOAuth2User(user, oauth2User);
    }
}
