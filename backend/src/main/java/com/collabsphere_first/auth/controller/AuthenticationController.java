package com.collabsphere_first.auth.controller;

import com.collabsphere_first.auth.dto.request.*;
import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import com.collabsphere_first.auth.exception.InvalidOtpException;
import com.collabsphere_first.auth.service.AuthenticationService;
import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.otp.repository.OtpRepository;
import com.collabsphere_first.security.oauth2.OAuth2CodeStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/v1/auth"})
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final OtpRepository otpRepository;
    private final OAuth2CodeStore oAuth2CodeStore;

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(
                authenticationService.login(request, response)
        );
    }

    @PostMapping({"/verify-otp", "/verify-email"})
    public ResponseEntity<MessageResponse> verifyOtp(
            @RequestBody Map<String, String> body) {
        String code = body.get("otp");
        if (code == null || code.isBlank()) {
            code = body.get("token");
        }
        String email = body.get("email");

        if ((email == null || email.isBlank()) && code != null) {
            final String lookupCode = code.trim();
            Otp otpEntity = otpRepository.findByOtp(lookupCode).orElse(null);
            if (otpEntity != null && otpEntity.getUser() != null) {
                email = otpEntity.getUser().getEmail();
            }
        }

        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email != null ? email.trim() : "")
                .otp(code != null ? code.trim() : "")
                .build();

        return ResponseEntity.ok(
                authenticationService.verifyOtp(request)
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(
            @RequestParam String email) {
        return ResponseEntity.ok(
                authenticationService.resendOtp(email)
        );
    }

    @PostMapping({"/refresh-token", "/refresh"})
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) Map<String, String> body) {
        if (body != null && body.containsKey("refreshToken")) {
            request.setAttribute("refreshToken", body.get("refreshToken"));
        }
        return ResponseEntity.ok(
                authenticationService.refreshToken(request, response)
        );
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<AuthenticationResponse> exchangeOAuth2Code(
            @RequestBody Map<String, String> body) {
        String code = body != null ? body.get("code") : null;
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code is required.");
        }
        AuthenticationResponse authResponse = oAuth2CodeStore.consumeCode(code.trim());
        if (authResponse == null) {
            throw new InvalidOtpException("Invalid or expired OAuth authorization code.");
        }
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestBody(required = false) LogoutRequest logoutRequest) {
        return ResponseEntity.ok(
                authenticationService.logout(request, response, logoutRequest)
        );
    }

    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(
            HttpServletRequest request,
            HttpServletResponse response) {
        return ResponseEntity.ok(
                authenticationService.logoutAll(request, response)
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(
                authenticationService.forgotPassword(request)
        );
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<MessageResponse> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(
                authenticationService.verifyResetOtp(request)
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(
                authenticationService.resetPassword(request)
        );
    }
}
