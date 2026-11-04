package com.collabsphere_first.integration.storage;

import com.collabsphere_first.integration.storage.impl.FirebaseStorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FirebaseStorageServiceImplTest {
    @TempDir Path temporaryDirectory;

    @Test
    void storesFilesAndGeneratesSignedUrlsWithoutMockTokens() throws Exception {
        FirebaseStorageServiceImpl storage = new FirebaseStorageServiceImpl(
                temporaryDirectory.toString(), "https://files.example.test", "test-signing-secret");

        assertEquals("communities/a.txt", storage.uploadFile("communities/a.txt", "hello".getBytes(), "text/plain"));
        assertArrayEquals("hello".getBytes(), Files.readAllBytes(temporaryDirectory.resolve("communities/a.txt")));

        String url = storage.generateSignedUrl("communities/a.txt", 5);
        assertTrue(url.startsWith("https://files.example.test/communities%2Fa.txt?expires="));
        assertTrue(url.contains("&signature="));
        assertFalse(url.contains("mock"));
    }

    @Test
    void rejectsDirectoryTraversal() {
        FirebaseStorageServiceImpl storage = new FirebaseStorageServiceImpl(
                temporaryDirectory.toString(), "https://files.example.test", "test-signing-secret");
        assertThrows(IllegalArgumentException.class, () -> storage.uploadFile("../secret.txt", new byte[0], "text/plain"));
    }
}
