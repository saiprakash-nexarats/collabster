package com.collabsphere_first.storage.service.impl;

import com.collabsphere_first.storage.config.StorageProperties;
import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.dto.StorageObjectResponse;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.dto.UploadTicketResponse;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.exception.InvalidUploadException;
import com.collabsphere_first.storage.exception.StorageAccessDeniedException;
import com.collabsphere_first.storage.exception.StorageException;
import com.collabsphere_first.storage.exception.StorageObjectNotFoundException;
import com.collabsphere_first.storage.exception.UploadExpiredException;
import com.collabsphere_first.storage.provider.ObjectStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.storage.service.StorageManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class StorageManagementServiceImpl implements StorageManagementService {

    private static final Set<String> DISALLOWED_EXTENSIONS = Set.of(
            "exe", "dll", "so", "bat", "cmd", "sh", "com", "vbs", "ps1", "jar", "war", "ear"
    );

    private final StorageProperties properties;
    private final StorageObjectRepository repository;
    private final ObjectStorageProvider storageProvider;
    private final String signingSecret;

    public StorageManagementServiceImpl(
            StorageProperties properties,
            StorageObjectRepository repository,
            ObjectStorageProvider storageProvider,
            @Value("${jwt.secret:default-secret-key-collobster-2026-auth}") String jwtSecretFallback
    ) {
        this.properties = properties;
        this.repository = repository;
        this.storageProvider = storageProvider;
        if (properties.getSigningSecret() != null && !properties.getSigningSecret().isBlank()) {
            this.signingSecret = properties.getSigningSecret();
        } else {
            this.signingSecret = jwtSecretFallback;
        }
    }

    @Override
    @Transactional
    public UploadTicketResponse requestUpload(UploadInitiationRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required to initiate upload");
        }

        if (request.getWorkspaceId() != null) {
            validateWorkspaceAuthorization(request.getWorkspaceId(), currentUserId);
        }

        String safeFilename = validateAndSanitizeFilename(request.getFilename());
        String contentType = validateAndNormalizeContentType(request.getContentType());
        long contentLength = validateContentLength(request.getContentLength());

        UUID objectId = UUID.randomUUID();
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storageKey = "uploads/" + currentUserId + "/" + datePrefix + "/" + objectId + "_" + safeFilename;

        Instant expiresAt = Instant.now().plus(properties.getUploadExpirationMinutes(), ChronoUnit.MINUTES);
        long expiresEpoch = expiresAt.getEpochSecond();
        String signature = sign(objectId + ":" + expiresEpoch);

        String uploadUrl = buildUploadUrl(objectId, expiresEpoch, signature);

        StorageObject storageObject = StorageObject.builder()
                .id(objectId)
                .ownerId(currentUserId)
                .workspaceId(request.getWorkspaceId())
                .storageProvider(storageProvider.getProviderType())
                .storageKey(storageKey)
                .filename(safeFilename)
                .contentType(contentType)
                .expectedBytes(contentLength)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        repository.save(storageObject);
        log.info("Created storage upload ticket: id={}, ownerId={}, key={}, expectedBytes={}", objectId, currentUserId, storageKey, contentLength);

        return UploadTicketResponse.builder()
                .id(objectId)
                .objectId(objectId)
                .uploadUrl(uploadUrl)
                .storageKey(storageKey)
                .filename(safeFilename)
                .contentType(contentType)
                .expectedBytes(contentLength)
                .expiresAt(expiresAt)
                .state(UploadState.PENDING.name())
                .build();
    }

    @Override
    @Transactional
    public void handleDirectUpload(UUID objectId, InputStream body, String contentType, Long contentLength, long expires, String signature) {
        if (objectId == null) {
            throw new InvalidUploadException("Object ID is required");
        }
        if (Instant.now().getEpochSecond() > expires) {
            throw new UploadExpiredException("Upload ticket has expired");
        }
        if (!verifySignature(objectId + ":" + expires, signature)) {
            throw new StorageAccessDeniedException("Invalid upload signature");
        }

        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        // Strict state checks
        if (storageObject.getUploadState() == UploadState.COMPLETED) {
            throw new InvalidUploadException("Upload ticket has already been completed and cannot be reused or overwritten");
        }
        if (storageObject.getUploadState() == UploadState.UPLOADING) {
            throw new InvalidUploadException("Upload ticket has already been claimed and an upload is currently in progress");
        }
        if (storageObject.getUploadState() == UploadState.EXPIRED || Instant.now().isAfter(storageObject.getExpiresAt())) {
            storageObject.setUploadState(UploadState.EXPIRED);
            repository.save(storageObject);
            throw new UploadExpiredException("Upload ticket has expired");
        }
        if (storageObject.getUploadState() == UploadState.FAILED) {
            throw new InvalidUploadException("Upload session previously failed and cannot be reused");
        }
        if (storageObject.getUploadState() == UploadState.QUARANTINED) {
            throw new StorageAccessDeniedException("Upload object is quarantined");
        }
        if (storageObject.getUploadState() != UploadState.PENDING) {
            throw new InvalidUploadException("Upload ticket is in invalid state: " + storageObject.getUploadState());
        }

        // Concurrency-safe atomic claim
        int claimed = repository.claimUploadSession(objectId, Instant.now());
        if (claimed == 0) {
            throw new InvalidUploadException("Upload session could not be claimed or is already in progress/completed");
        }
        storageObject.setUploadState(UploadState.UPLOADING);

        // Exact Content-Length header validation
        if (contentLength != null && contentLength > 0 && !contentLength.equals(storageObject.getExpectedBytes())) {
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new InvalidUploadException("Uploaded Content-Length " + contentLength + " does not match expected size " + storageObject.getExpectedBytes());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            CountingInputStream countingStream = new CountingInputStream(new DigestInputStream(body, digest), storageObject.getExpectedBytes());

            storageProvider.storeObject(storageObject.getStorageKey(), countingStream,
                    contentType != null ? contentType : storageObject.getContentType(),
                    storageObject.getExpectedBytes());

            long actualBytes = countingStream.getBytesRead();
            if (actualBytes != storageObject.getExpectedBytes()) {
                storageProvider.deleteObject(storageObject.getStorageKey());
                storageObject.setUploadState(UploadState.FAILED);
                repository.save(storageObject);
                throw new InvalidUploadException("Uploaded file size (" + actualBytes + " bytes) does not match expected size (" + storageObject.getExpectedBytes() + " bytes)");
            }

            byte[] hashBytes = digest.digest();
            String checksum = HexFormat.of().formatHex(hashBytes);
            storageObject.setChecksum(checksum);
            storageObject.setVerifiedBytes(actualBytes);
            repository.save(storageObject);

            log.info("Stored direct upload bytes for object: {}, bytes: {}, checksum: {}", objectId, actualBytes, checksum);
        } catch (InvalidUploadException iue) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw iue;
        } catch (NoSuchAlgorithmException e) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (Exception ex) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new StorageException("Failed to process direct upload stream", ex);
        }
    }

    @Override
    @Transactional
    public StorageObjectResponse completeUpload(UUID objectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required to complete upload");
        }
        if (objectId == null) {
            throw new InvalidUploadException("Object ID is required");
        }

        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (!storageObject.getOwnerId().equals(currentUserId)) {
            throw new StorageAccessDeniedException("Unauthorized: User does not own this storage object");
        }

        // Idempotent repeat completion
        if (storageObject.getUploadState() == UploadState.COMPLETED) {
            log.info("Upload object {} is already completed, returning existing metadata", objectId);
            return toResponse(storageObject);
        }

        if (storageObject.getUploadState() == UploadState.PENDING) {
            throw new InvalidUploadException("No uploaded content found on storage backend for object: " + objectId);
        }

        if (storageObject.getUploadState() == UploadState.FAILED) {
            throw new InvalidUploadException("Upload session previously failed and cannot be completed");
        }

        if (storageObject.getUploadState() == UploadState.EXPIRED || Instant.now().isAfter(storageObject.getExpiresAt())) {
            storageObject.setUploadState(UploadState.EXPIRED);
            repository.save(storageObject);
            throw new UploadExpiredException("Upload ticket has expired");
        }

        if (storageObject.getUploadState() != UploadState.UPLOADING) {
            throw new InvalidUploadException("Upload object is in invalid state for completion: " + storageObject.getUploadState());
        }

        // Verify physical file on storage provider
        if (!storageProvider.objectExists(storageObject.getStorageKey())) {
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new InvalidUploadException("No uploaded content found on storage backend for object: " + objectId);
        }

        long actualBytes = storageProvider.getObjectSize(storageObject.getStorageKey());
        if (actualBytes != storageObject.getExpectedBytes()) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new InvalidUploadException("Stored file size (" + actualBytes + " bytes) does not match expected size (" + storageObject.getExpectedBytes() + " bytes)");
        }

        // Re-verify SHA-256 checksum from stored object
        String providerChecksum = computeChecksum(storageObject.getStorageKey());
        if (storageObject.getChecksum() != null && !storageObject.getChecksum().equalsIgnoreCase(providerChecksum)) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            storageObject.setUploadState(UploadState.FAILED);
            repository.save(storageObject);
            throw new InvalidUploadException("Stored file checksum verification mismatch at completion");
        }
        storageObject.setChecksum(providerChecksum);

        storageObject.setUploadState(UploadState.COMPLETED);
        storageObject.setVerifiedBytes(actualBytes);
        storageObject.setCompletedAt(Instant.now());
        repository.save(storageObject);

        log.info("Successfully completed upload for object: {}, verified bytes: {}", objectId, actualBytes);
        return toResponse(storageObject);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageObjectResponse getMetadata(UUID objectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required");
        }
        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (!storageObject.getOwnerId().equals(currentUserId)) {
            throw new StorageAccessDeniedException("Unauthorized to view this storage object");
        }
        return toResponse(storageObject);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageFileResource downloadFile(UUID objectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required to download file");
        }
        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (!storageObject.getOwnerId().equals(currentUserId)) {
            throw new StorageAccessDeniedException("Unauthorized to download this storage object");
        }
        if (storageObject.getUploadState() != UploadState.COMPLETED) {
            throw new InvalidUploadException("Storage object is not in COMPLETED state: " + storageObject.getUploadState());
        }

        InputStream stream = storageProvider.retrieveObject(storageObject.getStorageKey());
        long size = storageObject.getVerifiedBytes() != null ? storageObject.getVerifiedBytes() : storageProvider.getObjectSize(storageObject.getStorageKey());

        return StorageFileResource.builder()
                .resource(new InputStreamResource(stream))
                .filename(storageObject.getFilename())
                .contentType(storageObject.getContentType())
                .contentLength(size)
                .build();
    }

    @Override
    @Transactional
    public void deleteFile(UUID objectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required");
        }
        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (!storageObject.getOwnerId().equals(currentUserId)) {
            throw new StorageAccessDeniedException("Unauthorized to delete this storage object");
        }

        storageObject.setIsDeleted(true);
        storageObject.setDeletedAt(Instant.now());
        repository.save(storageObject);

        storageProvider.deleteObject(storageObject.getStorageKey());
        log.info("Deleted storage object: id={}, ownerId={}", objectId, currentUserId);
    }

    private void validateWorkspaceAuthorization(UUID workspaceId, Long currentUserId) {
        if (workspaceId == null) {
            return;
        }
        log.warn("Workspace-scoped upload rejected for workspaceId {}: user {} not authorized", workspaceId, currentUserId);
        throw new StorageAccessDeniedException("Workspace-scoped uploads require verified workspace membership. Unauthorized workspace: " + workspaceId);
    }

    private String validateAndSanitizeFilename(String rawFilename) {
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new InvalidUploadException("Filename is required");
        }
        String clean = java.nio.file.Path.of(rawFilename).getFileName().toString();
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.isBlank() || clean.equals(".")) {
            throw new InvalidUploadException("Invalid filename");
        }

        int dotIndex = clean.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < clean.length() - 1) {
            String ext = clean.substring(dotIndex + 1).toLowerCase();
            if (DISALLOWED_EXTENSIONS.contains(ext)) {
                throw new InvalidUploadException("Disallowed file extension: ." + ext);
            }
        }
        return clean;
    }

    private String validateAndNormalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new InvalidUploadException("ContentType is required");
        }
        String normalized = contentType.trim().toLowerCase();
        if (normalized.contains("x-msdownload") || normalized.contains("x-sh") || normalized.contains("x-bat") || normalized.contains("x-dosexec")) {
            throw new InvalidUploadException("Disallowed executable MIME type: " + contentType);
        }
        if (!properties.getAllowedMimeTypes().contains(normalized) && !normalized.equals("application/octet-stream")) {
            throw new InvalidUploadException("MIME type not allowed: " + contentType);
        }
        return normalized;
    }

    private long validateContentLength(Long contentLength) {
        if (contentLength == null || contentLength <= 0) {
            throw new InvalidUploadException("Content length must be positive");
        }
        if (contentLength > properties.getMaxFileSizeBytes()) {
            throw new InvalidUploadException("File size " + contentLength + " exceeds maximum allowed limit " + properties.getMaxFileSizeBytes());
        }
        return contentLength;
    }

    private String buildUploadUrl(UUID objectId, long expiresEpoch, String signature) {
        String base = properties.getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            base = base.replaceAll("/+$", "");
            return base + "/v1/storage/raw/" + objectId + "?expires=" + expiresEpoch + "&signature=" + signature;
        }
        return "/v1/storage/raw/" + objectId + "?expires=" + expiresEpoch + "&signature=" + signature;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign upload URL", e);
        }
    }

    private boolean verifySignature(String value, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        try {
            String expected = sign(value);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private String computeChecksum(String storageKey) {
        try (InputStream is = storageProvider.retrieveObject(storageKey)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) {
                digest.update(buf, 0, r);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new StorageException("Failed to compute checksum for object: " + storageKey, e);
        }
    }

    private StorageObjectResponse toResponse(StorageObject obj) {
        return StorageObjectResponse.builder()
                .id(obj.getId())
                .objectId(obj.getId())
                .filename(obj.getFilename())
                .contentType(obj.getContentType())
                .verifiedBytes(obj.getVerifiedBytes() != null ? obj.getVerifiedBytes() : obj.getExpectedBytes())
                .expectedBytes(obj.getExpectedBytes())
                .state(obj.getUploadState().name())
                .storageKey(obj.getStorageKey())
                .downloadUrl("/v1/storage/files/" + obj.getId() + "/download")
                .checksum(obj.getChecksum())
                .createdAt(obj.getCreatedAt())
                .completedAt(obj.getCompletedAt())
                .build();
    }

    private static class CountingInputStream extends FilterInputStream {
        private final long expectedBytes;
        private long bytesRead = 0;

        protected CountingInputStream(InputStream in, long expectedBytes) {
            super(in);
            this.expectedBytes = expectedBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                checkLimit();
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int count = super.read(b, off, len);
            if (count != -1) {
                bytesRead += count;
                checkLimit();
            }
            return count;
        }

        private void checkLimit() {
            if (bytesRead > expectedBytes) {
                throw new InvalidUploadException("Streamed bytes exceed expected size of " + expectedBytes + " bytes");
            }
        }

        public long getBytesRead() {
            return bytesRead;
        }
    }
}
