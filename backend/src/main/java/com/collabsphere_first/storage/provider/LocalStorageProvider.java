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
import java.nio.file.*;
import java.util.UUID;

/**
 * Local filesystem-backed ObjectStorageProvider.
 * Implements strict root containment, symlink attack detection, safe temporary-file staging (.part),
 * non-executable file permissions, and overwrite prevention with verified atomic moves.
 */
@Slf4j
@Component
public class LocalStorageProvider implements ObjectStorageProvider {

    private final Path rootPath;
    private final FileContentInspector contentInspector;

    public LocalStorageProvider(StorageProperties properties, FileContentInspector contentInspector) {
        this.contentInspector = contentInspector;
        String dir = properties.getUploadDir();
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
            Path parentDir = targetPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
                // Verify real path of parent directory does not escape storage root via symlinks
                Path realParent = parentDir.toRealPath();
                Path realRoot = rootPath.toRealPath();
                if (!realParent.startsWith(realRoot)) {
                    throw new InvalidUploadException("Parent directory escapes storage root via symlink: " + storageKey);
                }
            }

            // Verify target destination does NOT already exist (prevent overwriting completed objects)
            if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new InvalidUploadException("Target file already exists on storage backend and cannot be overwritten: " + storageKey);
            }

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

            // File content inspection & security check against approved MIME type
            contentInspector.inspectFileContent(tempPath, contentType);

            // Ensure stored file is not executable
            tempPath.toFile().setExecutable(false, false);

            // Verified publication: check destination does not exist immediately prior to move
            if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                throw new InvalidUploadException("Destination already exists prior to atomic publication: " + storageKey);
            }

            // Atomic publication without blind replacement
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicEx) {
                // Fallback for filesystems that do not support ATOMIC_MOVE across volume boundaries.
                // Verify destination does not exist and perform safe move without replacement.
                if (Files.exists(targetPath, LinkOption.NOFOLLOW_LINKS)) {
                    throw new InvalidUploadException("Destination already exists on move fallback: " + storageKey);
                }
                Files.move(tempPath, targetPath);
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
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            return 0L;
        }
        try {
            return Files.size(targetPath);
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

        // Walk path components to detect symbolic links
        Path check = resolved;
        while (check != null && !check.equals(rootPath)) {
            if (Files.exists(check, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(check)) {
                throw new InvalidUploadException("Symbolic link detected in path: " + storageKey);
            }
            check = check.getParent();
        }

        return resolved;
    }
}
