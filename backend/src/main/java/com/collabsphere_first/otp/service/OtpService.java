package com.collabsphere_first.otp.service;
import org.springframework.transaction.annotation.Transactional;

import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.otp.repository.OtpRepository;
import com.collabsphere_first.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;

    public String generateOtp() {

        return String.format("%06d", RANDOM.nextInt(1_000_000));

    }
@Transactional
    public Otp createOtp(User user) {

        otpRepository.deleteByUser(user);

        Otp otp = Otp.builder()
                .otp(generateOtp())
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        return otpRepository.save(otp);

    }

    public Otp getOtp(User user) {

        return otpRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("OTP not found"));

    }

    public boolean isExpired(Otp otp) {

        return otp.getExpiresAt()
                .isBefore(LocalDateTime.now());

    }
    @Transactional
    public void deleteOtp(User user) {

        otpRepository.deleteByUser(user);

    }
    @Transactional
    public Otp save(Otp otp) {
        return otpRepository.save(otp);
    }



}