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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Component
public class LocalStorageProvider implements ObjectStorageProvider {

    private final Path rootPath;
    private final FileContentInspector contentInspector;

    public LocalStorageProvider(StorageProperties storageProperties, FileContentInspector contentInspector) {
        this.contentInspector = contentInspector;
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
        Path tempPath = null;
        try {
            Files.createDirectories(targetPath.getParent());
            // Safe temporary-file writes: stage file in .part file first
            tempPath = targetPath.getParent().resolve(".upload_" + UUID.randomUUID() + ".part");

            try (OutputStream os = Files.newOutputStream(tempPath,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            // File content inspection & security check
            contentInspector.inspectFileContent(tempPath, contentType);

            // Ensure stored file is not executable
            tempPath.toFile().setExecutable(false, false);

            // Atomic publication
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicEx) {
                // Fallback if filesystem does not support ATOMIC_MOVE
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug("Stored verified object at key: {}, size: {}", storageKey, Files.size(targetPath));
        } catch (InvalidUploadException iue) {
            cleanupTemp(tempPath);
            throw iue;
        } catch (IOException e) {
            cleanupTemp(tempPath);
            throw new StorageException("Failed to write storage object to disk: " + storageKey, e);
        } catch (Exception ex) {
            cleanupTemp(tempPath);
            throw ex;
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

    private void cleanupTemp(Path tempPath) {
        if (tempPath != null) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
        }
    }

    private Path resolveSafePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidUploadException("Storage key cannot be empty");
        }
        String normalizedKey = storageKey.replace('\\', '/').replaceAll("^/+", "");
        if (normalizedKey.contains("../") || normalizedKey.equals("..") || normalizedKey.contains("/..")) {
            throw new InvalidUploadException("Path traversal attempt in storage key: " + storageKey);
        }
        Path resolved = rootPath.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(rootPath)) {
            throw new InvalidUploadException("Resolved path escapes configured storage root: " + storageKey);
        }
        return resolved;
    }
}
