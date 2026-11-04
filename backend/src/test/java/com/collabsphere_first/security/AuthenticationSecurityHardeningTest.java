package com.collabsphere_first.security;

import com.collabsphere_first.auth.controller.AuthenticationController;
import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.service.AuthenticationService;
import com.collabsphere_first.otp.repository.OtpRepository;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.oauth2.OAuth2CodeStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
    @DisplayName("SEC-003 & P1-01: OAuth2CodeStore provides single-use, browser-bound authorization codes")
    void oAuth2CodeStoreSingleUseAndFingerprintBinding() {
        OAuth2CodeStore codeStore = new OAuth2CodeStore();
        AuthenticationResponse auth = AuthenticationResponse.builder()
                .accessToken("sample-access-token-12345")
                .tokenType("Bearer")
                .email("user@example.com")
                .profileId(99L)
                .build();

        String browserFingerprint = "192.168.1.100|Mozilla/5.0";

        // Case 1: Mismatched browser fingerprint fails and burns the code
        String attackCode = "auth-code-attack";
        codeStore.saveCode(attackCode, auth, browserFingerprint, 60);
        AuthenticationResponse hijacked = codeStore.consumeCode(attackCode, "attacker-ip|AttackerAgent");
        assertThat(hijacked).isNull();
        // Verifying it is burned
        assertThat(codeStore.consumeCode(attackCode, browserFingerprint)).isNull();

        // Case 2: Legitimate browser with matching fingerprint succeeds on first attempt
        String validCode = "auth-code-valid";
        codeStore.saveCode(validCode, auth, browserFingerprint, 60);
        AuthenticationResponse consumed = codeStore.consumeCode(validCode, browserFingerprint);
        assertThat(consumed).isNotNull();
        assertThat(consumed.getAccessToken()).isEqualTo("sample-access-token-12345");

        // Replay attempt fails immediately (single-use)
        AuthenticationResponse replay = codeStore.consumeCode(validCode, browserFingerprint);
        assertThat(replay).isNull();
    }

    @Test
    @DisplayName("P0-02: JwtCookieService enforces Secure cookies by default in production and under HTTPS")
    void jwtCookieServiceProductionDefaults() {
        JwtCookieService cookieService = new JwtCookieService();
        // default profile is prod
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Invoking two-argument overload (used by authenticateUser) must still default to Secure in prod
        cookieService.addRefreshTokenCookie(response, "prod-refresh-token");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=None");
    }

    @Test
    @DisplayName("P0-02: JwtCookieService respects dev profile over plain HTTP")
    void jwtCookieServiceDevProfileHttp() {
        JwtCookieService cookieService = new JwtCookieService();
        ReflectionTestUtils.setField(cookieService, "activeProfiles", "dev");
        ReflectionTestUtils.setField(cookieService, "defaultProfiles", "dev");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.addRefreshTokenCookie(request, response, "dev-refresh-token");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).doesNotContain("Secure;");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("P1-03: verifyOtp rejects requests without an explicit email address")
    void verifyOtpRequiresEmail() {
        AuthenticationService authService = mock(AuthenticationService.class);
        OtpRepository otpRepo = mock(OtpRepository.class);
        OAuth2CodeStore codeStore = mock(OAuth2CodeStore.class);
        AuthenticationController controller = new AuthenticationController(authService, otpRepo, codeStore);

        assertThatThrownBy(() -> controller.verifyOtp(Map.of("otp", "123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");

        assertThatThrownBy(() -> controller.verifyOtp(Map.of("email", "test@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Verification code/token is required");
    }
}
