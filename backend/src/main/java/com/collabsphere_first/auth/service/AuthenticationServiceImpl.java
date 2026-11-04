package com.collabsphere_first.auth.service;
import com.collabsphere_first.auth.dto.request.*;
import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.security.jwt.JwtCookieService;
import com.collabsphere_first.security.jwt.AuthenticationTokenService;
import com.collabsphere_first.security.userdetails.UserPrincipal;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import com.collabsphere_first.auth.exception.InvalidOtpException;
import com.collabsphere_first.auth.exception.OtpExpiredException;
import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.user.entity.User;

import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.exception.RoleNotFoundException;
import com.collabsphere_first.auth.exception.UserAlreadyExistsException;
import com.collabsphere_first.mail.service.EmailService;
import com.collabsphere_first.otp.service.OtpService;
import com.collabsphere_first.refreshtoken.repository.RefreshTokenRepository;
import com.collabsphere_first.role.entity.Role;
import com.collabsphere_first.role.entity.RoleName;
import com.collabsphere_first.role.repository.RoleRepository;
import com.collabsphere_first.security.jwt.JwtService;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import com.collabsphere_first.notification.service.NotificationDeviceService;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.collabsphere_first.security.service.AccountRevocationCoordinator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationTokenService authenticationTokenService;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    private final EmailService emailService;
    private final JwtCookieService jwtCookieService;
    private final NotificationDeviceService notificationDeviceService;
    private final MessagingPresenceService presenceService;
    private final AccountRevocationCoordinator accountRevocationCoordinator;


    @Override
    public MessageResponse register(RegisterRequest request) {
        request.normalize();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "User already exists with this email."
            );
        }

        Role guestRole = roleRepository.findByName(RoleName.ROLE_GUEST)
                .orElseThrow(() ->
                        new RoleNotFoundException("ROLE_GUEST not found."));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .emailVerified(false)
                .accountEnabled(false)
                .build();

        user.getRoles().add(guestRole);

        User savedUser = userRepository.save(user);

        Otp otp = otpService.createOtp(savedUser);

        try {
            emailService.sendVerificationOtpEmail(
                    savedUser.getEmail(),
                    savedUser.getFirstName(),
                    otp.getOtp()
            );
        } catch (Exception ignored) {
            // Logged or handled gracefully in environments where external SMTP is absent
        }

        return MessageResponse.builder()
                .message("Registration successful. Please verify your email using the OTP sent to your email.")
                .email(savedUser.getEmail())
                .profileId(savedUser.getId())
                .build();
    }

    @Override
    public AuthenticationResponse login(
            LoginRequest request,
            HttpServletResponse response) {
        return login(request, null, response);
    }

    @Override
    public AuthenticationResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        return authenticationTokenService.authenticateUser(
                user,
                httpRequest,
                response
        );

    }

    @Override
    public MessageResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        Otp otp = otpService.getOtp(user);

        if (otpService.isExpired(otp)) {

            otpService.deleteOtp(user);

            throw new OtpExpiredException("OTP has expired.");

        }

        if (!otp.getOtp().equals(request.getOtp())) {
            throw new InvalidOtpException("Invalid OTP.");
        }

        user.setEmailVerified(true);
        user.setAccountEnabled(true);

        userRepository.save(user);

        otpService.deleteOtp(user);

        return MessageResponse.builder()
                .message("Email verified successfully.")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException("Email is already verified.");
        }

        Otp otp = otpService.createOtp(user);

        emailService.sendVerificationOtpEmail(
                user.getEmail(),
                user.getFirstName(),
                otp.getOtp()
        );

        return MessageResponse.builder()
                .message("A new OTP has been sent to your email.")
                .build();

    }

    @Override
    @Transactional(noRollbackFor = {IllegalArgumentException.class})
    public AuthenticationResponse refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = jwtCookieService.getRefreshToken(request);

        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token not found.");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token."));

        if (storedToken.getRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked.");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {

            refreshTokenRepository.delete(storedToken);

            throw new IllegalArgumentException("Refresh token has expired.");
        }

        String tokenType = jwtService.extractTokenType(refreshToken);
        if ("ACCESS".equalsIgnoreCase(tokenType)) {
            throw new IllegalArgumentException("Provided token is an access token, not a refresh token.");
        }

        User user = storedToken.getUser();

        Long tokenAuthVersion = jwtService.extractAuthVersion(refreshToken);
        long tokenVersion = (tokenAuthVersion != null) ? tokenAuthVersion : 0L;
        long currentAuthVersion = (user.getAuthVersion() != null) ? user.getAuthVersion() : 0L;
        if (tokenVersion != currentAuthVersion) {
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("Refresh token auth version is outdated.");
        }

        if (!Boolean.TRUE.equals(user.getAccountEnabled())
                || Boolean.TRUE.equals(user.getDeactivated())
                || Boolean.TRUE.equals(user.getDeleted())) {
            refreshTokenRepository.delete(storedToken);
            throw new IllegalArgumentException("User account is disabled, deactivated, or deleted.");
        }

        UserPrincipal userPrincipal = new UserPrincipal(user);

        String newAccessToken =
                jwtService.generateAccessToken(userPrincipal);

        String newRefreshToken =
                jwtService.generateRefreshToken(userPrincipal);

        storedToken.setToken(newRefreshToken);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        storedToken.setRevoked(false);

        refreshTokenRepository.save(storedToken);

        jwtCookieService.addRefreshTokenCookie(
                request,
                response,
                newRefreshToken
        );

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse logout(
            HttpServletRequest request,
            HttpServletResponse response,
            LogoutRequest logoutRequest) {

        String refreshToken = jwtCookieService.getRefreshToken(request);
        Optional<RefreshToken> storedTokenOpt = Optional.empty();

        if (refreshToken != null) {
            storedTokenOpt = refreshTokenRepository.findByToken(refreshToken);
            storedTokenOpt.ifPresent(refreshTokenRepository::delete);
        }

        // Determine authenticated user identity safely without trusting arbitrary caller parameters
        Long userId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getUser().getId();
        } else if (storedTokenOpt.isPresent()) {
            userId = storedTokenOpt.get().getUser().getId();
        }

        // Scoped current-device push-token revocation
        if (logoutRequest != null && userId != null) {
            if (logoutRequest.getDeviceId() != null) {
                // Ownership validation enforced inside notificationDeviceService.
                // Rejects with 403 Forbidden if device belongs to another user.
                notificationDeviceService.revokeDevice(logoutRequest.getDeviceId(), userId);
            } else if (logoutRequest.getPushToken() != null && !logoutRequest.getPushToken().isBlank()) {
                notificationDeviceService.revokeDeviceByToken(logoutRequest.getPushToken());
            }
        }

        jwtCookieService.clearRefreshTokenCookie(request, response);

        return MessageResponse.builder()
                .message("Logged out successfully.")
                .build();
    }

    @Override
    @Transactional
    public MessageResponse logoutAll(
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            user = principal.getUser();
        }

        if (user == null) {
            String refreshToken = jwtCookieService.getRefreshToken(request);
            if (refreshToken != null) {
                user = refreshTokenRepository.findByToken(refreshToken)
                        .map(RefreshToken::getUser)
                        .orElse(null);
            }
        }

        if (user == null) {
            throw new AccessDeniedException("Authentication required to logout from all devices.");
        }

        // 1. Revoke all refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // 2. Revoke all notification device tokens for this user
        notificationDeviceService.revokeAllForUser(user.getId());

        // 3. Evict presence for this user
        presenceService.forceOffline(user.getId());

        // 4. Coordinate account-wide session revocation (increments authVersion, closes local WebSockets, publishes Redis event)
        if (accountRevocationCoordinator != null) {
            accountRevocationCoordinator.revokeAccountSessions(user.getId(), "Logged out from all devices");
        }

        // 5. Clear current session cookie
        jwtCookieService.clearRefreshTokenCookie(request, response);

        return MessageResponse.builder()
                .message("Logged out from all devices successfully.")
                .build();
    }
    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalStateException(
                    "Please verify your email before resetting your password."
            );
        }

        Otp otp = otpService.createOtp(user);

        emailService.sendPasswordResetOtpEmail(
                user.getEmail(),
                user.getFirstName(),
                otp.getOtp()
        );

        return MessageResponse.builder()
                .message("OTP sent successfully to your registered email.")
                .build();

    }
    @Override
    @Transactional
    public MessageResponse verifyResetOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        Otp otp = otpService.getOtp(user);

        if (otpService.isExpired(otp)) {

            otpService.deleteOtp(user);

            throw new OtpExpiredException("OTP has expired.");

        }

        if (!otp.getOtp().equals(request.getOtp())) {

            throw new InvalidOtpException("Invalid OTP.");

        }

        otp.setVerified(true);

        otpService.save(otp);

        return MessageResponse.builder()
                .message("OTP verified successfully.")
                .build();

    }
    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found."));

        Otp otp = otpService.getOtp(user);

        if (!otp.getVerified()) {
            throw new InvalidOtpException(
                    "Please verify your OTP first."
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        otpService.deleteOtp(user);
        refreshTokenRepository.deleteByUser(user);

        return MessageResponse.builder()
                .message("Password reset successfully.")
                .build();

    }


}
