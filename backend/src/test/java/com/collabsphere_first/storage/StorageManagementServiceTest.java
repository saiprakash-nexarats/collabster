package com.collabsphere_first.storage;

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
import com.collabsphere_first.storage.exception.StorageObjectNotFoundException;
import com.collabsphere_first.storage.exception.UploadExpiredException;
import com.collabsphere_first.storage.provider.FileContentInspector;
import com.collabsphere_first.storage.provider.LocalStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import com.collabsphere_first.storage.service.impl.StorageManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stage 5A — Storage Management Service Tests")
class StorageManagementServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private StorageObjectRepository repository;

    private StorageProperties properties;
    private LocalStorageProvider localStorageProvider;
    private StorageManagementServiceImpl storageService;

    private final Long ownerId = 100L;
    private final Long outsiderId = 200L;
    private final String signingSecret = "test-secret-key-storage-stage-5a-audit";

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
            byte[] hash = mac.doFinal((objectId + ":" + expiresEpoch).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Upload Initiation (POST /v1/storage/uploads)")
    class RequestUploadTests {

        @Test
        @DisplayName("Valid upload initiation returns ticket with signed URL and saves PENDING record")
        void requestUpload_success() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("my-portfolio.pdf")
                    .contentType("application/pdf")
                    .contentLength(1024L)
                    .build();

            when(repository.save(any(StorageObject.class))).thenAnswer(inv -> inv.getArgument(0));

            UploadTicketResponse ticket = storageService.requestUpload(req, ownerId);

            assertThat(ticket).isNotNull();
            assertThat(ticket.getId()).isNotNull();
            assertThat(ticket.getObjectId()).isEqualTo(ticket.getId());
            assertThat(ticket.getFilename()).isEqualTo("my-portfolio.pdf");
            assertThat(ticket.getContentType()).isEqualTo("application/pdf");
            assertThat(ticket.getExpectedBytes()).isEqualTo(1024L);
            assertThat(ticket.getState()).isEqualTo("PENDING");
            assertThat(ticket.getUploadUrl()).contains("expires=").contains("signature=");
            verify(repository, times(1)).save(any(StorageObject.class));
        }

        @Test
        @DisplayName("Unauthenticated upload request is rejected with 403 Forbidden")
        void requestUpload_unauthenticated_rejected() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .contentLength(500L)
                    .build();

            assertThatThrownBy(() -> storageService.requestUpload(req, null))
                    .isInstanceOf(StorageAccessDeniedException.class)
                    .hasMessageContaining("Authenticated user required");
        }

        @Test
        @DisplayName("Unauthorized workspace ID in upload request is rejected with 403")
        void requestUpload_unauthorizedWorkspace_rejected() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .contentLength(500L)
                    .workspaceId(UUID.randomUUID())
                    .build();

            assertThatThrownBy(() -> storageService.requestUpload(req, ownerId))
                    .isInstanceOf(StorageAccessDeniedException.class)
                    .hasMessageContaining("Workspace-scoped uploads require verified workspace membership");
        }

        @Test
        @DisplayName("Disallowed executable file extension (.exe, .sh, .bat) is rejected")
        void requestUpload_disallowedExtension_rejected() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("malicious.exe")
                    .contentType("application/octet-stream")
                    .contentLength(500L)
                    .build();

            assertThatThrownBy(() -> storageService.requestUpload(req, ownerId))
                    .isInstanceOf(InvalidUploadException.class)
                    .hasMessageContaining("Disallowed file extension");
        }

        @Test
        @DisplayName("Oversized content length (> 25MB) is rejected")
        void requestUpload_oversized_rejected() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("huge_video.mp4")
                    .contentType("video/mp4")
                    .contentLength(30 * 1024 * 1024L)
                    .build();

            assertThatThrownBy(() -> storageService.requestUpload(req, ownerId))
                    .isInstanceOf(InvalidUploadException.class)
                    .hasMessageContaining("exceeds maximum allowed limit");
        }

        @Test
        @DisplayName("Invalid or blank filename is rejected")
        void requestUpload_blankFilename_rejected() {
            UploadInitiationRequest req = UploadInitiationRequest.builder()
                    .filename("   ")
                    .contentType("text/plain")
                    .contentLength(100L)
                    .build();

            assertThatThrownBy(() -> storageService.requestUpload(req, ownerId))
                    .isInstanceOf(InvalidUploadException.class);
        }
    }

    @Nested
    @DisplayName("Direct PUT Upload & Completion Lifecycle")
    class UploadLifecycleTests {

        @Test
        @DisplayName("Full workflow: request upload -> direct PUT upload -> complete upload -> download")
        void fullLifecycle_success() {
            when(repository.save(any(StorageObject.class))).thenAnswer(inv -> inv.getArgument(0));

            // 1. Request upload initiation
            UploadInitiationRequest initReq = UploadInitiationRequest.builder()
                    .filename("test.txt")
                    .contentType("text/plain")
                    .contentLength(13L)
                    .build();
            UploadTicketResponse ticket = storageService.requestUpload(initReq, ownerId);

            String query = ticket.getUploadUrl().substring(ticket.getUploadUrl().indexOf('?') + 1);
            long expires = Long.parseLong(query.split("&")[0].split("=")[1]);
            String signature = query.split("&")[1].split("=")[1];

            // 2. Direct upload using ticket's generated objectId
            UUID ticketId = ticket.getId();
            StorageObject ticketObject = StorageObject.builder()
                    .id(ticketId)
                    .ownerId(ownerId)
                    .storageProvider(StorageProvider.LOCAL)
                    .storageKey(ticket.getStorageKey())
                    .filename("test.txt")
                    .contentType("text/plain")
                    .expectedBytes(13L)
                    .uploadState(UploadState.PENDING)
                    .expiresAt(ticket.getExpiresAt())
                    .isDeleted(false)
                    .build();
            when(repository.findByIdAndIsDeletedFalse(ticketId)).thenReturn(Optional.of(ticketObject));
            when(repository.claimUploadSession(eq(ticketId), any(Instant.class))).thenReturn(1);

            byte[] fileBytes = "Hello Collab!".getBytes(StandardCharsets.UTF_8);
            storageService.handleDirectUpload(ticketId, new ByteArrayInputStream(fileBytes), "text/plain", 13L, expires, signature);

            // Verify file exists on disk
            assertThat(localStorageProvider.objectExists(ticket.getStorageKey())).isTrue();
            assertThat(localStorageProvider.getObjectSize(ticket.getStorageKey())).isEqualTo(fileBytes.length);

            // 3. Complete upload
            StorageObjectResponse completed = storageService.completeUpload(ticketId, ownerId);
            assertThat(completed.getState()).isEqualTo("COMPLETED");
            assertThat(completed.getVerifiedBytes()).isEqualTo(fileBytes.length);
            assertThat(completed.getChecksum()).isNotBlank();

            // 4. Download file as owner
            StorageFileResource download = storageService.downloadFile(ticketId, ownerId);
            assertThat(download.getFilename()).isEqualTo("test.txt");
            assertThat(download.getContentLength()).isEqualTo(fileBytes.length);
        }

        @Test
        @DisplayName("Direct PUT with tampered or invalid signature is rejected with 403")
        void handleDirectUpload_invalidSignature_rejected() {
            UUID objectId = UUID.randomUUID();
            long expires = Instant.now().plus(15, ChronoUnit.MINUTES).getEpochSecond();
            byte[] bytes = "Test data".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(bytes),
                    "text/plain", (long) bytes.length, expires, "invalid-tampered-signature"))
                    .isInstanceOf(StorageAccessDeniedException.class)
                    .hasMessageContaining("Invalid upload signature");
        }

        @Test
        @DisplayName("Direct PUT with expired ticket is rejected with UploadExpiredException")
        void handleDirectUpload_expiredTicket_rejected() {
            UUID objectId = UUID.randomUUID();
            long expiredTime = Instant.now().minus(5, ChronoUnit.MINUTES).getEpochSecond();
            byte[] bytes = "Test data".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(bytes),
                    "text/plain", (long) bytes.length, expiredTime, "some-sig"))
                    .isInstanceOf(UploadExpiredException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Direct PUT on completed object is rejected (prevents overwriting)")
        void handleDirectUpload_alreadyCompleted_rejected() {
            UUID objectId = UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
            long expires = expiresAt.getEpochSecond();

            StorageObject completedObject = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("uploads/completed.txt")
                    .filename("completed.txt")
                    .contentType("text/plain")
                    .expectedBytes(10L)
                    .verifiedBytes(10L)
                    .uploadState(UploadState.COMPLETED)
                    .expiresAt(expiresAt)
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(completedObject));
            String signature = generateSignature(objectId, expires);

            byte[] bytes = "New content".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(bytes),
                    "text/plain", (long) bytes.length, expires, signature))
                    .isInstanceOf(InvalidUploadException.class)
                    .hasMessageContaining("already been completed and cannot be reused or overwritten");
        }

        @Test
        @DisplayName("Direct PUT with mismatched Content-Length header is rejected")
        void handleDirectUpload_contentLengthMismatch_rejected() {
            UUID objectId = UUID.randomUUID();
            Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
            long expires = expiresAt.getEpochSecond();

            StorageObject pendingObject = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("uploads/pending.txt")
                    .filename("pending.txt")
                    .contentType("text/plain")
                    .expectedBytes(100L)
                    .uploadState(UploadState.PENDING)
                    .expiresAt(expiresAt)
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(pendingObject));
            when(repository.claimUploadSession(eq(objectId), any(Instant.class))).thenReturn(1);
            String signature = generateSignature(objectId, expires);

            byte[] bytes = "short".getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> storageService.handleDirectUpload(objectId, new ByteArrayInputStream(bytes),
                    "text/plain", 50L, expires, signature))
                    .isInstanceOf(InvalidUploadException.class)
                    .hasMessageContaining("does not match expected size");
        }

        @Test
        @DisplayName("Different unauthorized user cannot complete another user's upload")
        void completeUpload_outsiderUser_rejected() {
            UUID objectId = UUID.randomUUID();
            StorageObject object = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("key-1")
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .expectedBytes(100L)
                    .uploadState(UploadState.UPLOADING)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(object));

            assertThatThrownBy(() -> storageService.completeUpload(objectId, outsiderId))
                    .isInstanceOf(StorageAccessDeniedException.class)
                    .hasMessageContaining("User does not own this storage object");
        }

        @Test
        @DisplayName("Completing upload without raw bytes stored on backend fails clearly")
        void completeUpload_missingContentOnBackend_rejected() {
            UUID objectId = UUID.randomUUID();
            StorageObject object = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("uploads/nonexistent-key.pdf")
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .expectedBytes(100L)
                    .uploadState(UploadState.PENDING)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(object));

            assertThatThrownBy(() -> storageService.completeUpload(objectId, ownerId))
                    .isInstanceOf(InvalidUploadException.class)
                    .hasMessageContaining("No uploaded content found");
        }

        @Test
        @DisplayName("Repeated completeUpload call is idempotent and safe")
        void completeUpload_repeatCall_isIdempotent() {
            UUID objectId = UUID.randomUUID();
            StorageObject completedObj = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("key-1")
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .expectedBytes(100L)
                    .verifiedBytes(100L)
                    .uploadState(UploadState.COMPLETED)
                    .completedAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(completedObj));

            StorageObjectResponse res = storageService.completeUpload(objectId, ownerId);
            assertThat(res.getState()).isEqualTo("COMPLETED");
            verify(repository, never()).save(any(StorageObject.class));
        }

        @Test
        @DisplayName("Different user cannot download another user's completed file")
        void downloadFile_outsiderUser_rejected() {
            UUID objectId = UUID.randomUUID();
            StorageObject completedObj = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("key-1")
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .expectedBytes(100L)
                    .verifiedBytes(100L)
                    .uploadState(UploadState.COMPLETED)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(completedObj));

            assertThatThrownBy(() -> storageService.downloadFile(objectId, outsiderId))
                    .isInstanceOf(StorageAccessDeniedException.class)
                    .hasMessageContaining("Unauthorized to download");
        }

        @Test
        @DisplayName("Owner can delete file safely")
        void deleteFile_owner_success() {
            UUID objectId = UUID.randomUUID();
            StorageObject obj = StorageObject.builder()
                    .id(objectId)
                    .ownerId(ownerId)
                    .storageKey("key-del")
                    .filename("doc.pdf")
                    .contentType("application/pdf")
                    .expectedBytes(100L)
                    .uploadState(UploadState.COMPLETED)
                    .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                    .isDeleted(false)
                    .build();

            when(repository.findByIdAndIsDeletedFalse(objectId)).thenReturn(Optional.of(obj));
            when(repository.save(any(StorageObject.class))).thenAnswer(inv -> inv.getArgument(0));

            storageService.deleteFile(objectId, ownerId);

            assertThat(obj.getIsDeleted()).isTrue();
            assertThat(obj.getDeletedAt()).isNotNull();
            verify(repository, times(1)).save(obj);
        }
    }
}
