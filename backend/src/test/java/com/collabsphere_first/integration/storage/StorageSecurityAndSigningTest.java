package com.collabsphere_first.integration.storage;

import com.collabsphere_first.integration.storage.impl.FirebaseStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageSecurityAndSigningTest {

    @TempDir
    Path tempDir;

    private FirebaseStorageServiceImpl storageService;
    private final String publicBaseUrl = "https://storage.collabster.internal/files";
    private final String signingSecret = "super-secure-production-hmac-secret-key-32b";

    @BeforeEach
    void setUp() {
        storageService = new FirebaseStorageServiceImpl(tempDir.toString(), publicBaseUrl, signingSecret);
    }

    @Test
    @DisplayName("Path traversal in storage key is blocked")
    void pathTraversal_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> storageService.generateSignedUrl("../../etc/passwd", 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traverse");

        assertThatThrownBy(() -> storageService.uploadFile("..\\..\\windows\\system32\\calc.exe", new byte[]{1, 2}, "application/octet-stream"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank storage key is rejected")
    void blankStorageKey_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> storageService.generateSignedUrl("", 60))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storageService.generateSignedUrl(null, 60))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Valid storage key generates signed URL with expiry and signature parameters")
    void generateSignedUrl_success() {
        String key = "communities/avatars/user-123.png";
        String signedUrl = storageService.generateSignedUrl(key, 30);

        assertThat(signedUrl)
                .startsWith(publicBaseUrl)
                .contains("expires=")
                .contains("signature=")
                .contains("communities%2Favatars%2Fuser-123.png");
    }

    @Test
    @DisplayName("Missing base URL or secret throws IllegalStateException")
    void missingConfig_throwsIllegalStateException() {
        FirebaseStorageServiceImpl unconfigured = new FirebaseStorageServiceImpl(tempDir.toString(), "", "");
        assertThatThrownBy(() -> unconfigured.generateSignedUrl("file.pdf", 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Storage signing requires");
    }

    @Test
    @DisplayName("Upload file stores byte contents cleanly in directory")
    void uploadFile_storesBytesSuccessfully() {
        byte[] payload = "Hello Collabster World".getBytes();
        String savedKey = storageService.uploadFile("resources/doc1.txt", payload, "text/plain");

        assertThat(savedKey).isEqualTo("resources/doc1.txt");
        assertThat(tempDir.resolve("resources/doc1.txt")).exists();
    }
}
