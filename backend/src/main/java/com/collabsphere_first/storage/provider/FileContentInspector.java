package com.collabsphere_first.storage.provider;

import com.collabsphere_first.storage.exception.InvalidUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Validates stored binary file content against approved MIME signatures and
 * prevents executable binary disguised uploads (PE, ELF, shebang scripts, Java class files).
 */
@Slf4j
@Component
public class FileContentInspector {

    private static final byte[] MZ_HEADER = new byte[]{(byte) 0x4D, (byte) 0x5A}; // DOS / Windows PE executable
    private static final byte[] ELF_HEADER = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46}; // Linux ELF executable
    private static final byte[] SHEBANG_HEADER = new byte[]{(byte) 0x23, (byte) 0x21}; // Shell shebang '#!'
    private static final byte[] JAVA_CLASS_HEADER = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}; // Java bytecode

    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] PDF_SIGNATURE = new byte[]{
            '%', 'P', 'D', 'F'
    };
    private static final byte[] JPEG_SIGNATURE = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF
    };

    public void inspectFileContent(Path filePath, String contentType) {
        if (filePath == null || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new InvalidUploadException("Cannot inspect nonexistent file: " + filePath);
        }

        byte[] header = new byte[64];
        int bytesRead;
        try (InputStream is = Files.newInputStream(filePath)) {
            bytesRead = is.read(header);
        } catch (IOException e) {
            throw new InvalidUploadException("Failed reading file content for security inspection: " + e.getMessage());
        }

        if (bytesRead <= 0) {
            throw new InvalidUploadException("Uploaded file contains 0 bytes and is empty");
        }

        // 1. Check for executable binary magic bytes
        if (startsWith(header, bytesRead, MZ_HEADER)) {
            throw new InvalidUploadException("Disallowed Windows executable binary format (MZ) detected in upload content");
        }
        if (startsWith(header, bytesRead, ELF_HEADER)) {
            throw new InvalidUploadException("Disallowed Unix/Linux ELF executable binary format detected in upload content");
        }
        if (startsWith(header, bytesRead, SHEBANG_HEADER)) {
            throw new InvalidUploadException("Disallowed executable script shebang ('#!') detected in upload content");
        }
        if (startsWith(header, bytesRead, JAVA_CLASS_HEADER)) {
            throw new InvalidUploadException("Disallowed compiled Java bytecode format detected in upload content");
        }

        // 2. Strict MIME type signature validation
        if (contentType != null && !contentType.isBlank()) {
            String normalizedMime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);

            switch (normalizedMime) {
                case "application/pdf" -> {
                    if (bytesRead < PDF_SIGNATURE.length || !startsWith(header, bytesRead, PDF_SIGNATURE)) {
                        throw new InvalidUploadException("Declared MIME type application/pdf does not match file signature (missing %PDF-) or is malformed");
                    }
                }
                case "image/png" -> {
                    if (bytesRead < PNG_SIGNATURE.length || !startsWith(header, bytesRead, PNG_SIGNATURE)) {
                        throw new InvalidUploadException("Declared MIME type image/png does not match PNG signature or is malformed");
                    }
                }
                case "image/jpeg", "image/jpg" -> {
                    if (bytesRead < JPEG_SIGNATURE.length || !startsWith(header, bytesRead, JPEG_SIGNATURE)) {
                        throw new InvalidUploadException("Declared MIME type image/jpeg does not match JPEG signature or is malformed");
                    }
                }
                case "image/gif" -> {
                    if (bytesRead < 6) {
                        throw new InvalidUploadException("Declared MIME type image/gif is truncated or malformed");
                    }
                    String magic = new String(header, 0, 6);
                    if (!magic.equals("GIF87a") && !magic.equals("GIF89a")) {
                        throw new InvalidUploadException("Declared MIME type image/gif does not match GIF signature (expected GIF87a or GIF89a)");
                    }
                }
                case "image/webp" -> {
                    if (bytesRead < 12) {
                        throw new InvalidUploadException("Declared MIME type image/webp is truncated or malformed");
                    }
                    boolean isRiff = header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F';
                    boolean isWebp = header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                    if (!isRiff || !isWebp) {
                        throw new InvalidUploadException("Declared MIME type image/webp does not match WEBP signature (missing RIFF/WEBP header)");
                    }
                }
                default -> log.debug("Permitting generic content type: {}", normalizedMime);
            }
        }
    }

    private boolean startsWith(byte[] data, int dataLen, byte[] prefix) {
        if (dataLen < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
