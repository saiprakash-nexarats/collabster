package com.collabsphere_first.messaging.repository;

import com.collabsphere_first.messaging.entity.UploadedMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadedMediaRepository extends JpaRepository<UploadedMedia, UUID> {

    Optional<UploadedMedia> findByStorageKey(String storageKey);

    List<UploadedMedia> findByOwnerUserId(Long ownerUserId);
}
