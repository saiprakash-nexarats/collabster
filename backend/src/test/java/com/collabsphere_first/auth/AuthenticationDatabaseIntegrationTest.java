package com.collabsphere_first.auth;

import com.collabsphere_first.auth.dto.request.LoginRequest;
import com.collabsphere_first.auth.dto.request.LogoutRequest;
import com.collabsphere_first.auth.dto.request.RegisterRequest;
import com.collabsphere_first.auth.dto.request.VerifyOtpRequest;
import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.exception.EmailDeliveryException;
import com.collabsphere_first.auth.exception.InvalidOtpException;
import com.collabsphere_first.auth.exception.OtpExpiredException;
import com.collabsphere_first.auth.exception.UserAlreadyExistsException;
import com.collabsphere_first.auth.service.AuthenticationServiceImpl;
import com.collabsphere_first.mail.service.EmailService;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.otp.repository.OtpRepository;
import com.collabsphere_first.otp.service.OtpService;
import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.jwt.AuthenticationTokenService;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.security.service.AccountRevocationCoordinator;
import com.collabsphere_first.security.userdetails.UserPrincipal;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Authentication Full Database Integration Lifecycle Tests")
class AuthenticationDatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private JwtService jwtService;
    private JwtCookieService jwtCookieService;
    private AuthenticationTokenService tokenService;
    private AuthenticationManager authenticationManager;
    private NotificationDeviceService notificationDeviceService;
    private MessagingPresenceService presenceService;
    private AccountRevocationCoordinator accountRevocationCoordinator;
    private OtpService otpService;

    private AuthenticationServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        emailService = mock(EmailService.class);
        jwtService = mock(JwtService.class);
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock-access-token-xyz");
        when(jwtService.generateRefreshToken(any(UserPrincipal.class))).thenReturn("mock-refresh-token-abc");

        jwtCookieService = new JwtCookieService();
        tokenService = new AuthenticationTokenService(jwtService, jwtCookieService, refreshTokenRepository);
        authenticationManager = mock(AuthenticationManager.class);
        notificationDeviceService = mock(NotificationDeviceService.class);
        presenceService = mock(MessagingPresenceService.class);
        accountRevocationCoordinator = mock(AccountRevocationCoordinator.class);

        otpService = new OtpService(otpRepository);

        authService = new AuthenticationServiceImpl(
                tokenService,
                userRepository,
                roleRepository,
                otpService,
                refreshTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                emailService,
                jwtCookieService,
                notificationDeviceService,
                presenceService,
                accountRevocationCoordinator
        );

        // Prepopulate ROLE_GUEST role in DB if absent
        if (roleRepository.findByName(RoleName.ROLE_GUEST).isEmpty()) {
            Role guestRole = Role.builder().name(RoleName.ROLE_GUEST).build();
            roleRepository.save(guestRole);
        }
    }

    @Test
    @DisplayName("1. User registration saves user in DB with hashed password and creates durable OTP record")
    void testRegistrationDatabasePersistence() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .username("alicesmith")
                .email("alice@example.com")
                .password("SecurePassword123!")
                .confirmPassword("SecurePassword123!")
                .build();

        MessageResponse resp = authService.register(req);
        assertThat(resp).isNotNull();
        assertThat(resp.getEmail()).isEqualTo("alice@example.com");

        // Verify database persistence
        Optional<User> userOpt = userRepository.findByEmail("alice@example.com");
        assertThat(userOpt).isPresent();
        User user = userOpt.get();
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(user.getAccountEnabled()).isFalse();
        assertThat(passwordEncoder.matches("SecurePassword123!", user.getPassword())).isTrue();

        // Verify OTP persisted in database
        Optional<Otp> otpOpt = otpRepository.findByUser(user);
        assertThat(otpOpt).isPresent();
        assertThat(otpOpt.get().getOtp()).hasSize(6);

        // Verify emailService called
        verify(emailService, times(1)).sendVerificationOtpEmail(eq("alice@example.com"), eq("Alice"), anyString());
    }

    @Test
    @DisplayName("2. Registration rejects duplicate email addresses")
    void testRegistrationDuplicateEmail() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Bob")
                .lastName("Jones")
                .username("bobjones")
                .email("bob@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        authService.register(req);

        // Attempt second registration with same email
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User already exists with this email.");
    }

    @Test
    @DisplayName("3. Registration rejects password and confirmPassword mismatch")
    void testRegistrationPasswordMismatch() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Charlie")
                .lastName("Brown")
                .username("charlie")
                .email("charlie@example.com")
                .password("Password123!")
                .confirmPassword("MismatchedPassword123!")
                .build();

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match.");
    }

    @Test
    @DisplayName("4. Email verification with valid OTP enables user account in DB and removes OTP")
    void testEmailVerificationSuccess() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("David")
                .lastName("Miller")
                .username("davidmiller")
                .email("david@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        authService.register(req);
        User user = userRepository.findByEmail("david@example.com").orElseThrow();
        Otp otp = otpRepository.findByUser(user).orElseThrow();

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email("david@example.com")
                .otp(otp.getOtp())
                .build();

        MessageResponse response = authService.verifyOtp(verifyReq);
        assertThat(response.getMessage()).isEqualTo("Email verified successfully.");

        // Verify user account in DB is enabled and verified
        User updatedUser = userRepository.findByEmail("david@example.com").orElseThrow();
        assertThat(updatedUser.getEmailVerified()).isTrue();
        assertThat(updatedUser.getAccountEnabled()).isTrue();

        // Verify OTP was deleted from DB (single use)
        assertThat(otpRepository.findByUser(updatedUser)).isEmpty();
    }

    @Test
    @DisplayName("5. Email verification with invalid OTP rejects and leaves account disabled")
    void testEmailVerificationInvalidOtp() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Emma")
                .lastName("Watson")
                .username("emmawatson")
                .email("emma@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        authService.register(req);

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email("emma@example.com")
                .otp("000000") // Wrong OTP
                .build();

        assertThatThrownBy(() -> authService.verifyOtp(verifyReq))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessageContaining("Invalid OTP.");

        User user = userRepository.findByEmail("emma@example.com").orElseThrow();
        assertThat(user.getEmailVerified()).isFalse();
    }

    @Test
    @DisplayName("6. Email verification with expired OTP rejects and purges expired OTP from DB")
    void testEmailVerificationExpiredOtp() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Frank")
                .lastName("Sinatra")
                .username("franksinatra")
                .email("frank@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        authService.register(req);
        User user = userRepository.findByEmail("frank@example.com").orElseThrow();
        Otp otp = otpRepository.findByUser(user).orElseThrow();

        // Artificially expire the OTP in database
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        otpRepository.save(otp);

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email("frank@example.com")
                .otp(otp.getOtp())
                .build();

        assertThatThrownBy(() -> authService.verifyOtp(verifyReq))
                .isInstanceOf(OtpExpiredException.class)
                .hasMessageContaining("OTP has expired.");

        // Verify expired OTP was purged from DB
        assertThat(otpRepository.findByUser(user)).isEmpty();
    }

    @Test
    @DisplayName("7. Resend OTP generates new OTP in DB and replaces old OTP")
    void testResendOtpReplacesPrevious() {
        RegisterRequest req = RegisterRequest.builder()
                .firstName("Grace")
                .lastName("Hopper")
                .username("gracehopper")
                .email("grace@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .build();

        authService.register(req);
        User user = userRepository.findByEmail("grace@example.com").orElseThrow();
        Otp originalOtp = otpRepository.findByUser(user).orElseThrow();
        String originalCode = originalOtp.getOtp();

        MessageResponse resendResp = authService.resendOtp("grace@example.com");
        assertThat(resendResp.getMessage()).isEqualTo("A new OTP has been sent to your email.");

        Otp newOtp = otpRepository.findByUser(user).orElseThrow();
        assertThat(newOtp.getOtp()).isNotEqualTo(originalCode);
    }

    @Test
    @DisplayName("8. Login creates refresh token in DB and attaches HttpOnly Secure Set-Cookie")
    void testLoginSetsCookieAndPersistsToken() {
        User user = User.builder()
                .firstName("Helen")
                .lastName("Keller")
                .username("helenkeller")
                .email("helen@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build();
        userRepository.save(user);

        LoginRequest loginReq = LoginRequest.builder()
                .email("helen@example.com")
                .password("Password123!")
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationResponse authResp = authService.login(loginReq, request, response);
        assertThat(authResp).isNotNull();
        assertThat(authResp.getAccessToken()).isEqualTo("mock-access-token-xyz");

        // Verify refreshToken persisted in database
        Optional<RefreshToken> storedToken = refreshTokenRepository.findByUser(user);
        assertThat(storedToken).isPresent();
        assertThat(storedToken.get().getToken()).isEqualTo("mock-refresh-token-abc");

        // Verify Set-Cookie header
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("refreshToken=mock-refresh-token-abc");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    @DisplayName("9. Refresh token rotation updates database with new token and issues rotated cookie")
    void testRefreshTokenRotation() {
        User user = User.builder()
                .firstName("Ian")
                .lastName("Malcolm")
                .username("ianmalcolm")
                .email("ian@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .authVersion(0L)
                .build();
        userRepository.save(user);

        RefreshToken initialToken = RefreshToken.builder()
                .token("initial-refresh-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(initialToken);

        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token-2");
        when(jwtService.generateRefreshToken(any())).thenReturn("rotated-refresh-token-2");
        when(jwtService.extractTokenType("initial-refresh-token")).thenReturn("REFRESH");
        when(jwtService.extractAuthVersion("initial-refresh-token")).thenReturn(0L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "initial-refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationResponse refreshResp = authService.refreshToken(request, response);
        assertThat(refreshResp.getAccessToken()).isEqualTo("new-access-token-2");

        // Verify DB updated with rotated token
        RefreshToken updatedToken = refreshTokenRepository.findByUser(user).orElseThrow();
        assertThat(updatedToken.getToken()).isEqualTo("rotated-refresh-token-2");

        // Verify new cookie set
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=rotated-refresh-token-2");
    }

    @Test
    @DisplayName("10. Logout deletes refresh token from database and issues Max-Age=0 cookie")
    void testLogoutDeletesTokenAndClearsCookie() {
        User user = User.builder()
                .firstName("Julia")
                .lastName("Roberts")
                .username("juliaroberts")
                .email("julia@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build();
        userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .token("session-refresh-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "session-refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        MessageResponse logoutResp = authService.logout(request, response, null);
        assertThat(logoutResp.getMessage()).isEqualTo("Logged out successfully.");

        // Verify token deleted from database
        assertThat(refreshTokenRepository.findByToken("session-refresh-token")).isEmpty();

        // Verify cookie clearing header
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    @DisplayName("11. Logout-all revokes all user refresh tokens in database and coordinates session invalidation")
    void testLogoutAllRevokesAllTokensInDatabase() {
        User user = User.builder()
                .firstName("Kevin")
                .lastName("Costner")
                .username("kevincostner")
                .email("kevin@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .accountEnabled(true)
                .build();
        userRepository.save(user);

        RefreshToken token = RefreshToken.builder()
                .token("kevin-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);

        UserPrincipal principal = new UserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        MessageResponse logoutAllResp = authService.logoutAll(request, response);
        assertThat(logoutAllResp.getMessage()).isEqualTo("Logged out from all devices successfully.");

        // Verify all tokens for this user purged from DB
        assertThat(refreshTokenRepository.findByUser(user)).isEmpty();

        // Verify revocation coordinator was notified
        verify(accountRevocationCoordinator).revokeAccountSessions(eq(user.getId()), anyString());

        // Verify cookie cleared
        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("Max-Age=0");
    }
}

