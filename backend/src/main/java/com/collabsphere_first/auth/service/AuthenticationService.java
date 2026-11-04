package com.collabsphere_first.auth.service;

import com.collabsphere_first.auth.dto.request.*;
import com.collabsphere_first.auth.dto.response.AuthenticationResponse;
import com.collabsphere_first.auth.dto.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationService {

    MessageResponse register(RegisterRequest request);

    default AuthenticationResponse login(
            LoginRequest request,
            HttpServletResponse response
    ) {
        return login(request, null, response);
    }

    AuthenticationResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    );

    MessageResponse verifyOtp(VerifyOtpRequest request);

    MessageResponse resendOtp(String email);

    AuthenticationResponse refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    );

    default MessageResponse logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return logout(request, response, null);
    }

    MessageResponse logout(
            HttpServletRequest request,
            HttpServletResponse response,
            LogoutRequest logoutRequest
    );

    MessageResponse logoutAll(
            HttpServletRequest request,
            HttpServletResponse response
    );

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse verifyResetOtp(VerifyOtpRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

}
