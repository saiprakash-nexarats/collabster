package com.collabsphere_first.storage;

import com.collabsphere_first.storage.config.StorageProperties;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.exception.InvalidUploadException;
import com.collabsphere_first.storage.provider.FileContentInspector;
import com.collabsphere_first.storage.provider.LocalStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.storage.service.StorageTransactionCoordinator;
import com.collabsphere_first.storage.service.impl.StorageManagementServiceImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:storage_db_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StorageTransactionCoordinator.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Stage 5A — Storage Real Database Persistence & Durable Rollback Prevention Tests")
public class StorageDatabaseIntegrationTest {

    @TempDir
    Path tempDir;

    @Autowired
    private StorageObjectRepository repository;

    @Autowired
    private StorageTransactionCoordinator transactionCoordinator;

    @Autowired
    private EntityManager entityManager;

    private StorageProperties properties;
    private LocalStorageProvider localStorageProvider;
    private StorageManagementServiceImpl storageService;

    private final Long ownerId = 42L;
    private final String signingSecret = "stage-5a-db-integration-hmac-secret-32b";

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        properties = new StorageProperties();
        properties.setUploadDir(tempDir.toString());
        properties.setPublicBaseUrl("http://localhost:8080");
        properties.setSigningSecret(signingSecret);
        properties.setMaxFileSizeBytes(25 * 1024 * 1024L);
        properties.setUploadExpirationMinutes(15);

        localStorageProvider = new LocalStorageProvider(properties, new FileContentInspector());
        storageService = new StorageManagementServiceImpl(properties, repository, localStorageProvider, transactionCoordinator, signingSecret);
    }

    private String generateSignature(UUID objectId, long expiresEpoch) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal((objectId + ":" + expiresEpoch).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Durable Rollback Prevention: Streaming size mismatch marks record FAILED in DB and does not roll back to PENDING")
    void streamingFailure_isDurableInDatabase_cannotRevertToPending() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expiresEpoch = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expiresEpoch);

        // 1. Persist initial PENDING record in real DB
        StorageObject entity = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/42/2026/11/06/" + objectId + "_test.txt")
                .filename("test.txt")
                .contentType("text/plain")
                .expectedBytes(100L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();
        repository.save(entity);

        // Verify initial DB state is PENDING
        StorageObject beforeUpload = repository.findById(objectId).orElseThrow();
        assertThat(beforeUpload.getUploadState()).isEqualTo(UploadState.PENDING);

        // 2. Stream only 5 bytes when 100 bytes are expected -> causes InvalidUploadException
        byte[] payload = "short".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storageService.handleDirectUpload(
                objectId, new ByteArrayInputStream(payload), "text/plain", 100L, expiresEpoch, signature
        )).isInstanceOf(InvalidUploadException.class);

        // 3. CRITICAL VERIFICATION: In the real database, the record MUST be durably FAILED
        StorageObject afterFailure = repository.findById(objectId).orElseThrow();
        assertThat(afterFailure.getUploadState())
                .as("Failed upload must commit FAILED state durably, never rolling back to PENDING")
                .isEqualTo(UploadState.FAILED);

        // 4. Ticket Replay Prevention: A subsequent upload attempt with the same valid HMAC ticket MUST be rejected
        assertThatThrownBy(() -> storageService.handleDirectUpload(
                objectId, new ByteArrayInputStream(payload), "text/plain", 100L, expiresEpoch, signature
        )).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("previously failed and cannot be reused");
    }

    @Test
    @DisplayName("Durable Rollback Prevention: Bad MIME type mismatch marks record FAILED in DB")
    void mimeTypeMismatch_durablyMarksFailedInDatabase() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expiresEpoch = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expiresEpoch);

        StorageObject entity = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/42/2026/11/06/" + objectId + "_document.pdf")
                .filename("document.pdf")
                .contentType("application/pdf")
                .expectedBytes(50L)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();
        repository.save(entity);

        byte[] payload = new byte[50];
        // Client attempts to PUT with image/png instead of approved application/pdf
        assertThatThrownBy(() -> storageService.handleDirectUpload(
                objectId, new ByteArrayInputStream(payload), "image/png", 50L, expiresEpoch, signature
        )).isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("does not match approved type");

        // Verify durably FAILED in database
        StorageObject inDb = repository.findById(objectId).orElseThrow();
        assertThat(inDb.getUploadState()).isEqualTo(UploadState.FAILED);
    }

    @Test
    @DisplayName("Full Persistence Lifecycle: PENDING -> UPLOADING -> COMPLETED with verified bytes and SHA-256 checksum")
    void fullPersistenceLifecycle_success() {
        UUID objectId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        long expiresEpoch = expiresAt.getEpochSecond();
        String signature = generateSignature(objectId, expiresEpoch);

        byte[] content = "Full persistence test payload for Stage 5A".getBytes(StandardCharsets.UTF_8);

        StorageObject entity = StorageObject.builder()
                .id(objectId)
                .ownerId(ownerId)
                .storageProvider(StorageProvider.LOCAL)
                .storageKey("uploads/42/2026/11/06/" + objectId + "_lifecycle.txt")
                .filename("lifecycle.txt")
                .contentType("text/plain")
                .expectedBytes((long) content.length)
                .uploadState(UploadState.PENDING)
                .expiresAt(expiresAt)
                .isDeleted(false)
                .build();
        repository.save(entity);

        // Upload stream
        storageService.handleDirectUpload(objectId, new ByteArrayInputStream(content), "text/plain", (long) content.length, expiresEpoch, signature);

        // Complete upload
        var response = storageService.completeUpload(objectId, ownerId);
        assertThat(response.getState()).isEqualTo("COMPLETED");
        assertThat(response.getVerifiedBytes()).isEqualTo((long) content.length);
        assertThat(response.getChecksum()).isNotBlank();

        // Verify reload from database confirms COMPLETED state and non-null completed_at
        StorageObject completedInDb = repository.findById(objectId).orElseThrow();
        assertThat(completedInDb.getUploadState()).isEqualTo(UploadState.COMPLETED);
        assertThat(completedInDb.getVerifiedBytes()).isEqualTo((long) content.length);
        assertThat(completedInDb.getCompletedAt()).isNotNull();
        assertThat(completedInDb.getChecksum()).isEqualTo(response.getChecksum());

        // Soft deletion marks is_deleted in DB
        storageService.deleteFile(objectId, ownerId);
        Optional<StorageObject> activeAfterDelete = repository.findByIdAndIsDeletedFalse(objectId);
        assertThat(activeAfterDelete).isEmpty();

        StorageObject rawAfterDelete = repository.findById(objectId).orElseThrow();
        assertThat(rawAfterDelete.getIsDeleted()).isTrue();
        assertThat(rawAfterDelete.getDeletedAt()).isNotNull();
    }
}
