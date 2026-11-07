package com.collabsphere_first.mentor.repository;

import com.collabsphere_first.mentor.entity.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MentorProfileRepository extends JpaRepository<MentorProfile, UUID> {
    Optional<MentorProfile> findByUserId(Long userId);
    List<MentorProfile> findByStateOrderByCreatedAtDesc(String state);
}

