package com.collabsphere_first.storage.service;

import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.dto.StorageObjectResponse;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.dto.UploadTicketResponse;

import java.io.InputStream;
import java.util.UUID;

public interface StorageManagementService {

    UploadTicketResponse requestUpload(UploadInitiationRequest request, Long currentUserId);

    void handleDirectUpload(UUID objectId, InputStream body, String contentType, Long contentLength, long expires, String signature);

    StorageObjectResponse completeUpload(UUID objectId, Long currentUserId);

    StorageObjectResponse getMetadata(UUID objectId, Long currentUserId);

    StorageFileResource downloadFile(UUID objectId, Long currentUserId);

    void deleteFile(UUID objectId, Long currentUserId);
}

