package com.collabsphere_first.integration.storage;

import java.util.UUID;

/**
 * Shared storage service abstraction for resource attachments, durable file storage, and signed URLs.
 */
public interface StorageService {

    String uploadFile(String destinationPath, byte[] fileBytes, String contentType);

    byte[] downloadFile(String storageKey);

    void deleteFile(String storageKey);

    String generateSignedUrl(String storageKey, int expirationMinutes);

    boolean verifySignedUrl(String storageKey, long expires, String signature);

    String sanitizeFilename(String filename);

    String buildCommunityResourceKey(UUID communityId, String filename);

    void validateFile(String filename, byte[] content, String contentType, long maxSizeBytes);
}
