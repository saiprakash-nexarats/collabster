package com.collabsphere_first.security;

import com.collabsphere_first.auth.controller.AuthenticationController;
import com.collabsphere_first.auth.dto.request.RegisterRequest;
import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.exception.EmailDeliveryException;
import com.collabsphere_first.auth.service.AuthenticationService;
import com.collabsphere_first.auth.service.AuthenticationServiceImpl;
import com.collabsphere_first.mail.service.EmailService;
import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.otp.repository.OtpRepository;
import com.collabsphere_first.otp.service.OtpService;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.jwt.AuthenticationTokenService;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.oauth2.OAuth2CodeStore;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    @DisplayName("SEC-003 & P1-01: OAuth2CodeStore provides single-use, browser-bound authorization codes in dev/test")
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
    @DisplayName("P1-01 & Item 4: OAuth2CodeStore in production forbids silent memory fallback when Redis is absent")
    void oAuth2CodeStoreProductionForbidsSilentMemoryFallback() {
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.profiles.active", "")).thenReturn("prod");
        when(env.getProperty("spring.profiles.default", "prod")).thenReturn("prod");

        OAuth2CodeStore codeStore = new OAuth2CodeStore(null, objectMapper, env);
        AuthenticationResponse auth = AuthenticationResponse.builder()
                .accessToken("prod-token")
                .tokenType("Bearer")
                .build();

        // In prod without Redis, saveCode MUST throw IllegalStateException instead of silently using memory
        assertThatThrownBy(() -> codeStore.saveCode("code-prod", auth, "fingerprint", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Shared Redis store is required for OAuth code exchange in production");

        // Similarly, consumeCode MUST throw IllegalStateException instead of falling back to memory
        assertThatThrownBy(() -> codeStore.consumeCode("code-prod", "fingerprint"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Shared Redis store is required for OAuth code exchange in production");
    }

    @Test
    @DisplayName("P1-01 & Item 4: OAuth2CodeStore utilizes atomic Redis getAndDelete for multi-instance exchange")
    @SuppressWarnings("unchecked")
    void oAuth2CodeStoreAtomicRedisGetAndDelete() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        OAuth2CodeStore codeStore = new OAuth2CodeStore(redisTemplate, objectMapper, null);
        AuthenticationResponse auth = AuthenticationResponse.builder()
                .accessToken("atomic-access-token")
                .tokenType("Bearer")
                .build();

        String fingerprint = "client-fp-123";
        OAuth2CodeStore.CachedAuth cached = new OAuth2CodeStore.CachedAuth(auth, fingerprint, System.currentTimeMillis() + 60000L);
        String json = objectMapper.writeValueAsString(cached);

        // Atomic getAndDelete from Redis
        when(ops.getAndDelete("oauth_code:code-atomic")).thenReturn(json);

        AuthenticationResponse consumed = codeStore.consumeCode("code-atomic", fingerprint);
        assertThat(consumed).isNotNull();
        assertThat(consumed.getAccessToken()).isEqualTo("atomic-access-token");
        verify(ops).getAndDelete("oauth_code:code-atomic");
    }

    @Test
    @DisplayName("P0-02 & Item 5: JwtCookieService enforces Secure and SameSite=None in production with exact Set-Cookie header")
    void jwtCookieServiceProductionDefaults() {
        JwtCookieService cookieService = new JwtCookieService();
        // default profile is prod
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.addRefreshTokenCookie(response, "prod-refresh-token");

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("refreshToken=prod-refresh-token");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=None");
        assertThat(setCookieHeader).contains("Max-Age=604800"); // 7 days
    }

    @Test
    @DisplayName("P0-02 & Item 5: JwtCookieService respects dev profile over plain HTTP")
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
        assertThat(setCookieHeader).contains("refreshToken=dev-refresh-token");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/");
        assertThat(setCookieHeader).doesNotContain("Secure;");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("P0-02 & Item 5: clearRefreshTokenCookie produces Max-Age=0 header")
    void jwtCookieServiceClearCookie() {
        JwtCookieService cookieService = new JwtCookieService();
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieService.clearRefreshTokenCookie(response);

        String setCookieHeader = response.getHeader("Set-Cookie");
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("Max-Age=0");
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

    @Test
    @DisplayName("Item 6: Email delivery failure during registration retries and throws EmailDeliveryException without leaking OTP")
    void registrationEmailDeliveryFailureThrowsWithoutOtpLeak() {
        UserRepository userRepo = mock(UserRepository.class);
        RoleRepository roleRepo = mock(RoleRepository.class);
        OtpService otpService = mock(OtpService.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);

        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        Role role = Role.builder().id(1L).name(RoleName.ROLE_GUEST).build();
        when(roleRepo.findByName(RoleName.ROLE_GUEST)).thenReturn(Optional.of(role));
        when(encoder.encode(anyString())).thenReturn("hashed-pw");

        User savedUser = User.builder().id(50L).email("unreached@example.com").firstName("Jane").build();
        when(userRepo.save(any(User.class))).thenReturn(savedUser);

        Otp otp = Otp.builder().id(1L).otp("987654").user(savedUser).build();
        when(otpService.createOtp(any(User.class))).thenReturn(otp);

        // Email sending fails on every attempt
        doThrow(new RuntimeException("SMTP Host Connection Timed Out"))
                .when(emailService).sendVerificationOtpEmail(anyString(), anyString(), anyString());

        AuthenticationServiceImpl service = new AuthenticationServiceImpl(
                mock(AuthenticationTokenService.class),
                userRepo,
                roleRepo,
                otpService,
                mock(com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository.class),
                encoder,
                mock(AuthenticationManager.class),
                mock(JwtService.class),
                emailService,
                mock(JwtCookieService.class),
                mock(com.collabsphere_first.notification.service.NotificationDeviceService.class),
                mock(com.collabsphere_first.messaging.presence.MessagingPresenceService.class),
                null
        );

        RegisterRequest request = RegisterRequest.builder()
                .email("unreached@example.com")
                .password("StrongPass123!")
                .confirmPassword("StrongPass123!")
                .firstName("Jane")
                .lastName("Doe")
                .username("janedoe")
                .build();

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("verification email failed to send");

        // Verify retried 2 times
        verify(emailService, times(2)).sendVerificationOtpEmail(eq("unreached@example.com"), eq("Jane"), eq("987654"));
        // Verify user was still saved (not lost)
        verify(userRepo).save(any(User.class));
    }

    @Test
    @DisplayName("Item 5: OriginValidationFilter blocks state-changing auth request from untrusted origin (CSRF defense)")
    void originValidationFilterBlocksUntrustedOrigin() throws Exception {
        com.collabsphere_first.security.config.OriginValidationFilter filter =
                new com.collabsphere_first.security.config.OriginValidationFilter("http://localhost:3000,http://localhost:5173");

        // Case 1: Untrusted origin on POST /api/v1/auth/refresh -> 403 Forbidden
        MockHttpServletRequest evilRequest = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        evilRequest.addHeader("Origin", "http://evil-attacker.com");
        MockHttpServletResponse evilResponse = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(evilRequest, evilResponse, chain);

        assertThat(evilResponse.getStatus()).isEqualTo(403);
        assertThat(evilResponse.getContentAsString()).contains("unauthorized origin");
        verify(chain, never()).doFilter(evilRequest, evilResponse);

        // Case 2: Trusted origin on POST /api/v1/auth/refresh -> 200 / continues chain
        MockHttpServletRequest goodRequest = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        goodRequest.addHeader("Origin", "http://localhost:5173");
        MockHttpServletResponse goodResponse = new MockHttpServletResponse();

        filter.doFilter(goodRequest, goodResponse, chain);

        verify(chain).doFilter(goodRequest, goodResponse);
    }

    @Test
    @DisplayName("Item 10: AuthExceptionHandler sanitizes generic unhandled exception and never leaks internal details")
    void authExceptionHandlerSanitizesUnhandledException() {
        com.collabsphere_first.auth.exception.AuthExceptionHandler handler =
                new com.collabsphere_first.auth.exception.AuthExceptionHandler();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/auth/login");
        Exception internalEx = new RuntimeException("SELECT * FROM users WHERE id=1 failed: table corrupted");

        org.springframework.http.ResponseEntity<Object> response = handler.handleException(internalEx, request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("message")).isEqualTo("An unexpected internal error occurred. Please try again later.");
        assertThat(body.get("message")).asString().doesNotContain("SELECT");
        assertThat(body.get("message")).asString().doesNotContain("table corrupted");
    }
}
