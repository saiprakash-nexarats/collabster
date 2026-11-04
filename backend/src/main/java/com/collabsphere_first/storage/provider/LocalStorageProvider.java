package com.collabsphere_first.storage.provider;

import com.collabsphere_first.storage.config.StorageProperties;
import com.collabsphere_first.storage.enums.StorageProvider;
import com.collabsphere_first.storage.exception.InvalidUploadException;
import com.collabsphere_first.storage.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class LocalStorageProvider implements ObjectStorageProvider {

    private final Path rootPath;

    public LocalStorageProvider(StorageProperties storageProperties) {
        String dir = storageProperties.getUploadDir();
        if (dir == null || dir.isBlank()) {
            dir = "uploads";
        }
        this.rootPath = Path.of(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            log.error("Could not create local storage root directory: {}", this.rootPath, e);
        }
    }

    @Override
    public StorageProvider getProviderType() {
        return StorageProvider.LOCAL;
    }

    @Override
    public void storeObject(String storageKey, InputStream inputStream, String contentType, long contentLength) {
        Path targetPath = resolveSafePath(storageKey);
        try {
            Files.createDirectories(targetPath.getParent());
            try (OutputStream os = Files.newOutputStream(targetPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            log.debug("Stored object at key: {}, size: {}", storageKey, Files.size(targetPath));
        } catch (IOException e) {
            throw new StorageException("Failed to write storage object to disk: " + storageKey, e);
        }
    }

    @Override
    public InputStream retrieveObject(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new StorageException("File not found on disk: " + storageKey);
        }
        try {
            return Files.newInputStream(targetPath, StandardOpenOption.READ);
        } catch (IOException e) {
            throw new StorageException("Failed to open input stream for storage object: " + storageKey, e);
        }
    }

    @Override
    public boolean objectExists(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);
        return Files.exists(targetPath) && Files.isRegularFile(targetPath);
    }

    @Override
    public long getObjectSize(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);
        try {
            if (Files.exists(targetPath) && Files.isRegularFile(targetPath)) {
                return Files.size(targetPath);
            }
            return 0L;
        } catch (IOException e) {
            log.warn("Failed to check file size for: {}", storageKey, e);
            return 0L;
        }
    }

    @Override
    public void deleteObject(String storageKey) {
        Path targetPath = resolveSafePath(storageKey);
        try {
            Files.deleteIfExists(targetPath);
            log.debug("Deleted object from disk: {}", storageKey);
        } catch (IOException e) {
            log.warn("Failed to delete object on disk: {}", storageKey, e);
        }
    }

    private Path resolveSafePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidUploadException("Storage key cannot be empty");
        }
        String normalizedKey = storageKey.replace('\\', '/').replaceAll("^/+", "");
        if (normalizedKey.contains("../") || normalizedKey.equals("..")) {
            throw new InvalidUploadException("Path traversal attempt in storage key: " + storageKey);
        }
        Path resolved = rootPath.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new InvalidUploadException("Resolved path escapes configured storage root: " + storageKey);
        }
        return resolved;
    }
}

