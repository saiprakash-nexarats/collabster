package com.collabsphere_first.storage.repository;

import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorageObjectRepository extends JpaRepository<StorageObject, UUID> {

    Optional<StorageObject> findByIdAndIsDeletedFalse(UUID id);

    Optional<StorageObject> findByStorageKeyAndIsDeletedFalse(String storageKey);

    List<StorageObject> findByOwnerIdAndIsDeletedFalse(Long ownerId);

    List<StorageObject> findByWorkspaceIdAndIsDeletedFalse(UUID workspaceId);

    List<StorageObject> findByUploadStateAndExpiresAtBefore(UploadState uploadState, Instant expiresAt);
}

