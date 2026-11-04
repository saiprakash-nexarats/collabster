package com.collabsphere_first.mail.service;

public interface EmailService {

    void sendVerificationOtpEmail(
            String to,
            String firstName,
            String otp
    );

    void sendPasswordResetOtpEmail(
            String to,
            String firstName,
            String otp
    );

}