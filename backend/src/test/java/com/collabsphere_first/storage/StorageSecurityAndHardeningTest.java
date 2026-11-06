package com.collabsphere_first.storage;

import com.collabsphere_first.storage.config.StorageProperties;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.exception.InvalidUploadException;
import com.collabsphere_first.storage.exception.StorageAccessDeniedException;
import com.collabsphere_first.storage.provider.FileContentInspector;
import com.collabsphere_first.storage.provider.LocalStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.storage.service.impl.StorageManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stage 5A Security Closure — Concurrency, Size Integrity & Hardening Tests")
class StorageSecurityAndHardeningTest {

    @TempDir
    Path tempDir;

    @Mock
    private StorageObjectRepository repository;

    private StorageProperties properties;
    private LocalStorageProvider localStorageProvider;
    private StorageManagementServiceImpl storageService;

    private final Long ownerId = 100L;
    private final String signingSecret = "test-secret-key-storage-stage-5a-closure";

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicBaseUrl("http://localhost:8080");
        properties.setSigningSecret(signingSecret);
        properties.setMaxFileSizeBytes(25 * 1024 * 1024L);
        properties.setUploadExpirationMinutes(15);

        localStorageProvider = new LocalStorageProvider(properties, new FileContentInspector());
        storageService = new StorageManagementServiceImpl(properties, repository, localStorageProvider, signingSecret);
    }

    private String generateSignature(UUID objectId, long expiresEpoch) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((objectId + ":" + expiresEpoch).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("1. Concurrency: Two simultaneous PUT requests on the same ticket allow exactly one to succeed")
    void concurrentUploads_onlyOneSucceeds() throws Exception {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expires = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expires);

        StorageObject pendingObject = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/" + ownerId + "/concurrent_test.txt")
                .filename("concurrent_test.txt")
                .contentType("text/plain")
                .expectedBytes(10L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(pendingObject));

        // Atomic claim simulation: exactly one thread gets claim=1, subsequent get claim=0
        AtomicInteger claimCounter = new AtomicInteger(0);
        when(repository.claimUploadSession(eq(objectId), any(Instant.class)))
                .thenAnswer(inv -> claimCounter.getAndIncrement() == 0 ? 1 : 0);

        byte[] payload1 = "1234567890".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "0987654321".getBytes(StandardCharsets.UTF_8);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            startLatch.await();
            try {
                storageService.handleDirectUpload(objectId, new ByteArrayInputStream(payload1), "text/plain", 10L, expires, signature);
                successCount.incrementAndGet();
            } catch (InvalidUploadException ex) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            startLatch.await();
            try {
                storageService.handleDirectUpload(objectId, new ByteArrayInputStream(payload2), "text/plain", 10L, expires, signature);
                successCount.incrementAndGet();
            } catch (InvalidUploadException ex) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        startLatch.countDown();
        f1.get();
        f2.get();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("2. Replay Prevention: Completed object cannot have its contents overwritten through the old ticket")
    void completedObject_cannotBeOverwrittenByOldTicket() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expires = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expires);

        StorageObject completedObject = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/" + ownerId + "/fixed_original.txt")
                .filename("fixed_original.txt")
                .contentType("text/plain")
                .expectedBytes(10L)
                .verifiedBytes(10L)
                .checksum("initial-sha256-hash")
                .uploadState(UploadState.COMPLETED)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(completedObject));

        byte[] maliciousReplacement = "0987654321".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(maliciousReplacement),
                "text/plain", 10L, expires, signature))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("already been completed and cannot be reused or overwritten");

        // Verify checksum is untouched
        assertThat(completedObject.getChecksum()).isEqualTo("initial-sha256-hash");
    }

    @Test
    @DisplayName("3. Size Integrity: Uploading shorter byte stream than expected fails and marks FAILED")
    void streamedBytesShorterThanExpected_fails() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expires = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expires);

        StorageObject pendingObject = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageKey("uploads/" + ownerId + "/short.txt")
                .filename("short.txt")
                .contentType("text/plain")
                .expectedBytes(20L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(pendingObject));
        when(repository.claimUploadSession(eq(objectId), any(Instant.class))).thenReturn(1);

        byte[] shortBytes = "short_10".getBytes(StandardCharsets.UTF_8); // 8 bytes instead of 20

        assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(shortBytes),
                "text/plain", 20L, expires, signature))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("does not match expected size");

        assertThat(pendingObject.getUploadState()).isEqualTo(UploadState.FAILED);
    }

    @Test
    @DisplayName("4. Size Integrity: Uploading longer byte stream than expected fails and cleans up")
    void streamedBytesLongerThanExpected_fails() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expires = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expires);

        StorageObject pendingObject = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageKey("uploads/" + ownerId + "/long.txt")
                .filename("long.txt")
                .contentType("text/plain")
                .expectedBytes(5L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(pendingObject));
        when(repository.claimUploadSession(eq(objectId), any(Instant.class))).thenReturn(1);

        byte[] longBytes = "excessive_bytes_here".getBytes(StandardCharsets.UTF_8); // 20 bytes instead of 5

        assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(longBytes),
                "text/plain", 5L, expires, signature))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("exceed expected size");

        assertThat(pendingObject.getUploadState()).isEqualTo(UploadState.FAILED);
    }

    @Test
    @DisplayName("5. Workspace Authorization: Arbitrary unverified workspace ID is rejected with 403 Forbidden")
    void arbitraryWorkspaceId_isRejected() {
        UploadInitiationRequest req = UploadInitiationRequest.builder()
                .filename("workspace-doc.pdf")
                .contentType("application/pdf")
                .contentLength(1024L)
                .workspaceId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() -> storageService.requestUpload(req, ownerId))
                .isInstanceOf(StorageAccessDeniedException.class)
                .hasMessageContaining("Workspace-scoped uploads require verified workspace membership");
    }

    @Test
    @DisplayName("6. Content Inspection: Executable Windows PE (MZ) binary in stream is detected and rejected")
    void executableBinaryInStream_isDetectedAndRejected() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expires = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expires);

        StorageObject pendingObject = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageKey("uploads/" + ownerId + "/disguised.pdf")
                .filename("disguised.pdf")
                .contentType("application/pdf")
                .expectedBytes(10L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();

        when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(pendingObject));
        when(repository.claimUploadSession(eq(objectId), any(Instant.class))).thenReturn(1);

        // Stream starting with 'MZ' magic bytes (disguised as PDF)
        byte[] mzPayload = new byte[]{'M', 'Z', 0, 1, 2, 3, 4, 5, 6, 7};

        assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(mzPayload),
                "application/pdf", 10L, expires, signature))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("Disallowed Windows executable binary format (MZ) detected");

        assertThat(pendingObject.getUploadState()).isEqualTo(UploadState.FAILED);
    }

    @Test
    @DisplayName("7. Provider Configuration: S3 configured when only local is supported fails fast on startup")
    void s3ProviderConfigured_failsFastOnStartup() {
        StorageProperties badConfig = new StorageProperties();
        badConfig.setProvider("s3");

        assertThatThrownBy(badConfig::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only 'local' object storage is supported in this deployment");
    }
}
