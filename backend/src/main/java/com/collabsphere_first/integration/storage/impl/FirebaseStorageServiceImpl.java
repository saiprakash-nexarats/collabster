package com.collabsphere_first.integration.storage.impl;

import com.collabsphere_first.integration.storage.FirebaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
public class FirebaseStorageServiceImpl implements FirebaseStorageService {

    private final Path uploadDirectory;
    private final String publicBaseUrl;
    private final String signingSecret;

    public FirebaseStorageServiceImpl(
            @Value("${file.upload-dir:uploads}") String uploadDirectory,
            @Value("${collabster.storage.public-base-url:}") String publicBaseUrl,
            @Value("${collabster.storage.signing-secret:}") String signingSecret) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        this.signingSecret = signingSecret == null ? "" : signingSecret;
    }

    @Override
    public String generateSignedUrl(String storageKey, int expirationMinutes) {
        String key = requireSafeKey(storageKey);
        if (publicBaseUrl.isBlank() || signingSecret.isBlank()) {
            throw new IllegalStateException("Storage signing requires collabster.storage.public-base-url and collabster.storage.signing-secret");
        }
        long expires = Instant.now().plusSeconds(Math.max(1, expirationMinutes) * 60L).getEpochSecond();
        String signature = sign(key + ":" + expires);
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        return publicBaseUrl + "/" + encodedKey + "?expires=" + expires + "&signature=" + signature;
    }

    @Override
    public String uploadFile(String destinationPath, byte[] fileBytes, String contentType) {
        String key = requireSafeKey(destinationPath);
        try {
            Path target = uploadDirectory.resolve(key).normalize();
            if (!target.startsWith(uploadDirectory)) throw new IllegalArgumentException("Storage path escapes the configured upload directory");
            Files.createDirectories(target.getParent());
            Files.write(target, fileBytes == null ? new byte[0] : fileBytes);
            log.info("Stored uploaded file at {} ({})", key, contentType == null ? "application/octet-stream" : contentType);
            return key;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not store uploaded file", exception);
        }
    }

    @Override
    public byte[] downloadFile(String storageKey) {
        String key = requireSafeKey(storageKey);
        try {
            Path target = uploadDirectory.resolve(key).normalize();
            if (!target.startsWith(uploadDirectory) || !Files.exists(target)) {
                throw new IllegalArgumentException("Resource file not found: " + key);
            }
            return Files.readAllBytes(target);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not read stored file", exception);
        }
    }

    @Override
    public void deleteFile(String storageKey) {
        String key = requireSafeKey(storageKey);
        try {
            Path target = uploadDirectory.resolve(key).normalize();
            if (!target.startsWith(uploadDirectory)) {
                throw new IllegalArgumentException("Storage path escapes configured directory");
            }
            Files.deleteIfExists(target);
            log.info("Deleted file at storage key: {}", key);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not delete stored file", exception);
        }
    }

    @Override
    public boolean verifySignedUrl(String storageKey, long expires, String signature) {
        if (signature == null || signature.isBlank() || signingSecret.isBlank()) {
            return false;
        }
        if (Instant.now().getEpochSecond() > expires) {
            log.warn("Storage signed URL expired for key: {}", storageKey);
            return false;
        }
        try {
            String key = requireSafeKey(storageKey);
            String expected = sign(key + ":" + expires);
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.warn("Failed verifying signed URL signature: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        String clean = Path.of(filename).getFileName().toString();
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.startsWith(".")) {
            clean = "file_" + clean;
        }
        return clean.isBlank() ? "unnamed_file" : clean;
    }

    @Override
    public String buildCommunityResourceKey(java.util.UUID communityId, String filename) {
        if (communityId == null) {
            throw new IllegalArgumentException("Community ID is required for storage key");
        }
        String safeName = sanitizeFilename(filename);
        return "communities/" + communityId + "/resources/" + java.util.UUID.randomUUID() + "-" + safeName;
    }

    @Override
    public void validateFile(String filename, byte[] content, String contentType, long maxSizeBytes) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }
        if (maxSizeBytes > 0 && content.length > maxSizeBytes) {
            throw new IllegalArgumentException("File exceeds maximum allowed size: " + maxSizeBytes + " bytes");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type is required");
        }
        String lower = contentType.toLowerCase();
        if (lower.contains("executable") || lower.contains("x-sh") || lower.contains("x-msdownload") || lower.contains("x-bat")) {
            throw new IllegalArgumentException("Disallowed executable content type: " + contentType);
        }
    }

    private String requireSafeKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) throw new IllegalArgumentException("Storage key is required");
        String key = storageKey.replace('\\', '/').replaceAll("^/+", "");
        if (key.contains("../") || key.equals("..")) throw new IllegalArgumentException("Storage key must not traverse directories");
        return key;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign storage URL", exception);
        }
    }
}
