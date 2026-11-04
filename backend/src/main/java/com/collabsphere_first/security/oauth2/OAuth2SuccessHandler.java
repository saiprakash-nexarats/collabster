package com.collabsphere_first.security.oauth2;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.security.jwt.AuthenticationTokenService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticationTokenService authenticationTokenService;
    private final UserRepository userRepository;
    private final OAuth2CodeStore oAuth2CodeStore;

    @Value("${collobster.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public static String computeClientFingerprint(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        String ua = request.getHeader("User-Agent");
        return (ip != null ? ip : "") + "|" + (ua != null ? ua : "");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        User user;

        if (oauth2User instanceof AppOidcUser appOidcUser) {
            user = appOidcUser.getUser();
        } else if (oauth2User instanceof AppOAuth2User appOAuth2User) {
            user = appOAuth2User.getUser();
        } else {
            String email = oauth2User.getAttribute("email");

            user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException("User not found."));
        }

        AuthenticationResponse authResponse =
         authenticationTokenService.authenticateUser(
                user,
                request,
                response
        );

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json") && !accept.contains("text/html")) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    """
                    {
                        "accessToken":"%s",
                        "tokenType":"Bearer"
                    }
                    """.formatted(authResponse.getAccessToken())
            );
            return;
        }

        String base = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl.replaceAll("/+$", "")
                : "http://localhost:5173";

        // SEC-003 & P1-01: Issue a browser-bound single-use one-time code (valid 60 seconds)
        String oneTimeCode = UUID.randomUUID().toString();
        String clientFingerprint = computeClientFingerprint(request);
        oAuth2CodeStore.saveCode(oneTimeCode, authResponse, clientFingerprint, 60);

        String redirectUrl = base + "/?oauth_code=" + oneTimeCode;
        response.sendRedirect(redirectUrl);
    }
}
