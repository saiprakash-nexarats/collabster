package com.collabsphere_first.security;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.oauth2.OAuth2CodeStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationSecurityHardeningTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("SEC-001: MessageResponse serialized to JSON never leaks an OTP field")
    void messageResponseNeverContainsOtpField() throws Exception {
        MessageResponse response = MessageResponse.builder()
                .message("Registration successful. Please verify your email using the OTP sent to your email.")
                .email("test@example.com")
                .profileId(42L)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("otp");
        assertThat(json).contains("test@example.com");
        assertThat(json).contains("Registration successful");
    }

    @Test
    @DisplayName("SEC-003: OAuth2CodeStore provides single-use, time-bounded authorization codes")
    void oAuth2CodeStoreSingleUseAndExpiration() {
        OAuth2CodeStore codeStore = new OAuth2CodeStore();
        AuthenticationResponse auth = AuthenticationResponse.builder()
                .accessToken("sample-access-token-12345")
                .tokenType("Bearer")
                .email("user@example.com")
                .profileId(99L)
                .build();

        String testCode = "auth-code-xyz";
        codeStore.saveCode(testCode, auth, 60);

        // First consume: succeeds
        AuthenticationResponse consumed = codeStore.consumeCode(testCode);
        assertThat(consumed).isNotNull();
        assertThat(consumed.getAccessToken()).isEqualTo("sample-access-token-12345");

        // Second consume: fails immediately (replay attack prevention)
        AuthenticationResponse replay = codeStore.consumeCode(testCode);
        assertThat(replay).isNull();

        // Expired code test
        codeStore.saveCode("expired-code", auth, -1);
        assertThat(codeStore.consumeCode("expired-code")).isNull();
    }

    @Test
    @DisplayName("SEC-002: JwtCookieService adds HttpOnly cookie with root path and secure attributes")
    void jwtCookieServiceAttributes() {
        JwtCookieService cookieService = new JwtCookieService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.addRefreshTokenCookie(request, response, "dummy-refresh-token");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=None");
    }
}

