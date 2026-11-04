package com.collabsphere_first.refreshtoken.repository;

import com.collabsphere_first.refreshtoken.entity.RefreshToken;
import com.collabsphere_first.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);

}
