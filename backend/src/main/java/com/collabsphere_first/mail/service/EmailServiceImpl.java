package com.collabsphere_first.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendVerificationOtpEmail(
            String to,
            String firstName,
            String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("CollabSphere - Email Verification");

        message.setText(
                "Hello " + firstName + ",\n\n" +

                        "Welcome to CollabSphere!\n\n" +

                        "Your email verification OTP is:\n\n" +

                        otp +

                        "\n\nThis OTP is valid for 10 minutes.\n\n" +

                        "If you did not create this account, please ignore this email.\n\n" +

                        "Regards,\nCollabSphere Team"
        );

        mailSender.send(message);

    }
    @Override
    public void sendPasswordResetOtpEmail(
            String to,
            String firstName,
            String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("CollabSphere - Password Reset");

        message.setText(
                "Hello " + firstName + ",\n\n" +

                        "We received a request to reset your password.\n\n" +

                        "Your password reset OTP is:\n\n" +

                        otp +

                        "\n\nThis OTP is valid for 10 minutes.\n\n" +

                        "If you did not request a password reset, please ignore this email.\n\n" +

                        "Regards,\nCollabSphere Team"
        );

        mailSender.send(message);

    }

}