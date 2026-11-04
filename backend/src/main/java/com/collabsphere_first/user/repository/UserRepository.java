package com.collabsphere_first.user.repository;

import com.collabsphere_first.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.authVersion = u.authVersion + 1 WHERE u.id = :userId")
    int incrementAuthVersion(@Param("userId") Long userId);

    @Query("SELECT u.authVersion FROM User u WHERE u.id = :userId")
    Optional<Long> findAuthVersionByUserId(@Param("userId") Long userId);

    @Query("SELECT u.id, u.authVersion, u.accountEnabled, u.deactivated, u.deleted FROM User u WHERE u.id IN :userIds")
    List<Object[]> findAuthStatesByUserIds(@Param("userIds") Collection<Long> userIds);
}