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
import com.collabsphere_first.storage.service.StorageTransactionCoordinator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Locale;
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
    private final StorageTransactionCoordinator transactionCoordinator;
    private final String signingSecret;

    @Autowired
    public StorageManagementServiceImpl(
            StorageProperties properties,
            StorageObjectRepository repository,
            ObjectStorageProvider storageProvider,
            StorageTransactionCoordinator transactionCoordinator,
            @Value("${jwt.secret:default-secret-key-collobster-2026-auth}") String jwtSecretFallback
    ) {
        this.properties = properties;
        this.repository = repository;
        this.storageProvider = storageProvider;
        this.transactionCoordinator = transactionCoordinator != null ? transactionCoordinator : new StorageTransactionCoordinator(repository);
        if (properties.getSigningSecret() != null && !properties.getSigningSecret().isBlank()) {
            this.signingSecret = properties.getSigningSecret();
        } else {
            this.signingSecret = jwtSecretFallback;
        }
    }

    public StorageManagementServiceImpl(
            StorageProperties properties,
            StorageObjectRepository repository,
            ObjectStorageProvider storageProvider,
            String signingSecret
    ) {
        this(properties, repository, storageProvider, new StorageTransactionCoordinator(repository), signingSecret);
    }

    @Override
    @Transactional
    public UploadTicketResponse requestUpload(UploadInitiationRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required to initiate upload");
        }

        validateUploadRequest(request);

        if (request.getWorkspaceId() != null) {
            if (!isUserAuthorizedForWorkspace(currentUserId, request.getWorkspaceId())) {
                log.warn("Workspace-scoped upload rejected for workspaceId {}: user {} not authorized",
                        request.getWorkspaceId(), currentUserId);
                throw new StorageAccessDeniedException("Workspace-scoped uploads require verified workspace membership: user " + currentUserId + " not authorized for workspace " + request.getWorkspaceId());
            }
        }

        UUID objectId = UUID.randomUUID();
        String safeFilename = sanitizeFilename(request.getFilename());
        String contentType = request.getContentType().toLowerCase().trim();
        long contentLength = request.getContentLength();

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storageKey = String.format("uploads/%d/%s/%s_%s", currentUserId, datePrefix, objectId, safeFilename);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getUploadExpirationMinutes(), ChronoUnit.MINUTES);
        long expiresEpoch = expiresAt.getEpochSecond();

        String signature = generateSignature(objectId + ":" + expiresEpoch);
        String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
        String uploadUrl = String.format("%s/v1/storage/raw/%s?expires=%d&signature=%s", baseUrl, objectId, expiresEpoch, signature);

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
        log.info("Created storage upload ticket: id={}, ownerId={}, key={}, expectedBytes={}",
                objectId, currentUserId, storageKey, contentLength);

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

    /**
     * Handles binary streaming upload directly from client.
     * Intentionally NOT @Transactional at method level to prevent holding active database connections
     * during long network file streaming. All state transitions (claim, fail, finalize) are executed
     * via StorageTransactionCoordinator in independent, immediate transactions (REQUIRES_NEW).
     */
    @Override
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
            transactionCoordinator.markFailed(objectId, "Upload ticket has expired");
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

        // Concurrency-safe atomic claim committed immediately
        int claimed = transactionCoordinator.claimSession(objectId, Instant.now());
        if (claimed == 0) {
            throw new InvalidUploadException("Upload session could not be claimed or is already in progress/completed");
        }

        // Strict MIME-type integrity: enforce match with approved Content-Type
        String approvedContentType = storageObject.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedPut = normalizeContentType(contentType);
            String normalizedApproved = normalizeContentType(approvedContentType);
            if (!normalizedPut.equals(normalizedApproved)) {
                transactionCoordinator.markFailed(objectId, "Content-Type mismatch: approved=" + approvedContentType + ", received=" + contentType);
                throw new InvalidUploadException("Uploaded Content-Type '" + contentType + "' does not match approved type '" + approvedContentType + "'");
            }
        }

        // Exact Content-Length header validation
        if (contentLength != null && contentLength > 0 && !contentLength.equals(storageObject.getExpectedBytes())) {
            transactionCoordinator.markFailed(objectId, "Content-Length header mismatch");
            throw new InvalidUploadException("Uploaded Content-Length " + contentLength + " does not match expected size " + storageObject.getExpectedBytes());
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            CountingInputStream countingStream = new CountingInputStream(new DigestInputStream(body, digest), storageObject.getExpectedBytes());

            // Always store and inspect with server-approved Content-Type, ignoring client overrides
            storageProvider.storeObject(storageObject.getStorageKey(), countingStream,
                    approvedContentType, storageObject.getExpectedBytes());

            long actualBytes = countingStream.getBytesRead();
            if (actualBytes != storageObject.getExpectedBytes()) {
                storageProvider.deleteObject(storageObject.getStorageKey());
                transactionCoordinator.markFailed(objectId, "File size mismatch: actual=" + actualBytes + ", expected=" + storageObject.getExpectedBytes());
                throw new InvalidUploadException("Uploaded file size (" + actualBytes + " bytes) does not match expected size (" + storageObject.getExpectedBytes() + " bytes)");
            }

            byte[] hashBytes = digest.digest();
            String checksum = HexFormat.of().formatHex(hashBytes);

            // Durably record stream success in isolated transaction
            transactionCoordinator.recordStreamSuccess(objectId, actualBytes, checksum);
            log.info("Stored direct upload bytes for object: {}, bytes: {}, checksum: {}", objectId, actualBytes, checksum);
        } catch (InvalidUploadException iue) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            transactionCoordinator.markFailed(objectId, iue.getMessage());
            throw iue;
        } catch (NoSuchAlgorithmException e) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            transactionCoordinator.markFailed(objectId, e.getMessage());
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (Exception ex) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            transactionCoordinator.markFailed(objectId, ex.getMessage());
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
            transactionCoordinator.markFailed(objectId, "Upload ticket has expired");
            throw new UploadExpiredException("Upload ticket has expired");
        }

        if (storageObject.getUploadState() != UploadState.UPLOADING) {
            throw new InvalidUploadException("Upload object is in invalid state for completion: " + storageObject.getUploadState());
        }

        // Verify physical file on storage provider
        if (!storageProvider.objectExists(storageObject.getStorageKey())) {
            transactionCoordinator.markFailed(objectId, "No physical file found on storage provider");
            throw new InvalidUploadException("No uploaded content found on storage backend for object: " + objectId);
        }

        long actualBytes = storageProvider.getObjectSize(storageObject.getStorageKey());
        if (actualBytes != storageObject.getExpectedBytes()) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            transactionCoordinator.markFailed(objectId, "Stored file size mismatch at completion");
            throw new InvalidUploadException("Stored file size (" + actualBytes + " bytes) does not match expected size (" + storageObject.getExpectedBytes() + " bytes)");
        }

        // Re-verify SHA-256 checksum from stored object
        String providerChecksum = computeChecksum(storageObject.getStorageKey());
        if (storageObject.getChecksum() != null && !storageObject.getChecksum().equalsIgnoreCase(providerChecksum)) {
            storageProvider.deleteObject(storageObject.getStorageKey());
            transactionCoordinator.markFailed(objectId, "Stored file checksum verification mismatch at completion");
            throw new InvalidUploadException("Stored file checksum verification mismatch at completion");
        }

        StorageObject completed = transactionCoordinator.completeSession(objectId, actualBytes, providerChecksum, Instant.now());
        log.info("Successfully completed upload for object: {}, verified bytes: {}", objectId, actualBytes);
        return toResponse(completed);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageFileResource downloadFile(UUID objectId, Long currentUserId) {
        if (objectId == null) {
            throw new InvalidUploadException("Object ID is required");
        }

        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (storageObject.getUploadState() != UploadState.COMPLETED) {
            throw new InvalidUploadException("Cannot download uncompleted file in state: " + storageObject.getUploadState());
        }

        // Authorization check: file owner or workspace member
        boolean isOwner = currentUserId != null && currentUserId.equals(storageObject.getOwnerId());
        boolean isWorkspaceMember = storageObject.getWorkspaceId() != null && currentUserId != null
                && isUserAuthorizedForWorkspace(currentUserId, storageObject.getWorkspaceId());

        if (!isOwner && !isWorkspaceMember) {
            throw new StorageAccessDeniedException("Unauthorized to download: User is not authorized to download file: " + objectId);
        }

        InputStream stream = storageProvider.retrieveObject(storageObject.getStorageKey());
        return StorageFileResource.builder()
                .resource(new InputStreamResource(stream))
                .filename(storageObject.getFilename())
                .contentType(storageObject.getContentType())
                .contentLength(storageObject.getVerifiedBytes() != null ? storageObject.getVerifiedBytes() : storageObject.getExpectedBytes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StorageObjectResponse getMetadata(UUID objectId, Long currentUserId) {
        if (objectId == null) {
            throw new InvalidUploadException("Object ID is required");
        }

        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        boolean isOwner = currentUserId != null && currentUserId.equals(storageObject.getOwnerId());
        boolean isWorkspaceMember = storageObject.getWorkspaceId() != null && currentUserId != null
                && isUserAuthorizedForWorkspace(currentUserId, storageObject.getWorkspaceId());

        if (!isOwner && !isWorkspaceMember) {
            throw new StorageAccessDeniedException("User is not authorized to view file metadata: " + objectId);
        }

        return toResponse(storageObject);
    }

    @Override
    @Transactional
    public void deleteFile(UUID objectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new StorageAccessDeniedException("Authenticated user required to delete file");
        }
        if (objectId == null) {
            throw new InvalidUploadException("Object ID is required");
        }

        StorageObject storageObject = repository.findByIdAndIsDeletedFalse(objectId)
                .orElseThrow(() -> new StorageObjectNotFoundException("Storage object not found: " + objectId));

        if (!storageObject.getOwnerId().equals(currentUserId)) {
            throw new StorageAccessDeniedException("Unauthorized: Only the file owner can delete this file");
        }

        storageObject.setIsDeleted(true);
        storageObject.setDeletedAt(Instant.now());
        repository.save(storageObject);

        storageProvider.deleteObject(storageObject.getStorageKey());
        log.info("Deleted storage object: id={}, ownerId={}", objectId, currentUserId);
    }

    public static String normalizeContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private boolean isUserAuthorizedForWorkspace(Long userId, UUID workspaceId) {
        return false;
    }

    private void validateUploadRequest(UploadInitiationRequest request) {
        if (request == null) {
            throw new InvalidUploadException("Upload request cannot be null");
        }
        if (request.getFilename() == null || request.getFilename().isBlank()) {
            throw new InvalidUploadException("Filename is required");
        }
        if (request.getContentType() == null || request.getContentType().isBlank()) {
            throw new InvalidUploadException("Content-Type is required");
        }
        if (request.getContentLength() == null || request.getContentLength() <= 0) {
            throw new InvalidUploadException("Content-Length must be greater than zero");
        }
        if (request.getContentLength() > properties.getMaxFileSizeBytes()) {
            throw new InvalidUploadException("File size exceeds maximum allowed limit of " + properties.getMaxFileSizeBytes() + " bytes");
        }

        String ext = getFileExtension(request.getFilename());
        if (DISALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new InvalidUploadException("Disallowed file extension: " + ext);
        }

        String normalizedContentType = request.getContentType().toLowerCase().trim();
        boolean isMimeAllowed = properties.getAllowedMimeTypes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(normalizedContentType) ||
                        (allowed.endsWith("/*") && normalizedContentType.startsWith(allowed.substring(0, allowed.length() - 2))));

        if (!isMimeAllowed) {
            throw new InvalidUploadException("Content-Type not allowed: " + request.getContentType());
        }
    }

    private String sanitizeFilename(String filename) {
        String clean = filename.replace('\\', '/');
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.length() > 200) {
            String ext = getFileExtension(clean);
            clean = clean.substring(0, 190) + (ext.isEmpty() ? "" : "." + ext);
        }
        return clean.isEmpty() ? "unnamed_file" : clean;
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx > 0 && idx < filename.length() - 1) {
            return filename.substring(idx + 1).toLowerCase();
        }
        return "";
    }

    private String generateSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new StorageException("Failed to generate upload signature", e);
        }
    }

    private boolean verifySignature(String data, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = generateSignature(data);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String computeChecksum(String storageKey) {
        try (InputStream is = storageProvider.retrieveObject(storageKey)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new StorageException("Failed to compute stored object checksum: " + storageKey, e);
        }
    }

    private StorageObjectResponse toResponse(StorageObject obj) {
        String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
        String downloadUrl = String.format("%s/v1/storage/files/%s/download", baseUrl, obj.getId());

        return StorageObjectResponse.builder()
                .id(obj.getId())
                .objectId(obj.getId())
                .ownerId(obj.getOwnerId())
                .workspaceId(obj.getWorkspaceId())
                .storageProvider(obj.getStorageProvider().name())
                .storageKey(obj.getStorageKey())
                .filename(obj.getFilename())
                .contentType(obj.getContentType())
                .expectedBytes(obj.getExpectedBytes())
                .verifiedBytes(obj.getVerifiedBytes())
                .checksum(obj.getChecksum())
                .state(obj.getUploadState().name())
                .downloadUrl(downloadUrl)
                .expiresAt(obj.getExpiresAt())
                .createdAt(obj.getCreatedAt())
                .completedAt(obj.getCompletedAt())
                .build();
    }

    private static class CountingInputStream extends FilterInputStream {
        private final long maxBytes;
        private long bytesRead = 0;

        protected CountingInputStream(InputStream in, long maxBytes) {
            super(in);
            this.maxBytes = maxBytes;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) {
                bytesRead++;
                if (bytesRead > maxBytes) {
                    throw new InvalidUploadException("Streamed content cannot exceed expected size limit of " + maxBytes + " bytes");
                }
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int r = super.read(b, off, len);
            if (r != -1) {
                bytesRead += r;
                if (bytesRead > maxBytes) {
                    throw new InvalidUploadException("Streamed content cannot exceed expected size limit of " + maxBytes + " bytes");
                }
            }
            return r;
        }

        public long getBytesRead() {
            return bytesRead;
        }
    }
}
