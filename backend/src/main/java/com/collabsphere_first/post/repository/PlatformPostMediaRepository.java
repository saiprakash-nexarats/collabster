package com.collabsphere_first.post.repository;

import com.collabsphere_first.post.entity.PlatformPostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlatformPostMediaRepository extends JpaRepository<PlatformPostMedia, UUID> {

    List<PlatformPostMedia> findByPostIdOrderByDisplayOrderAsc(UUID postId);

    void deleteByPostId(UUID postId);
}
