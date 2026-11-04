package com.collabsphere_first.otp.repository;

import com.collabsphere_first.otp.entity.Otp;
import com.collabsphere_first.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findByUser(User user);

    Optional<Otp> findByOtp(String otp);

    void deleteByUser(User user);

}