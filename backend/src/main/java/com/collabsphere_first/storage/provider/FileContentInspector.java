package com.collabsphere_first.storage.provider;

import com.collabsphere_first.storage.exception.InvalidUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class FileContentInspector {

    private static final byte[] MZ_HEADER = new byte[]{(byte) 0x4D, (byte) 0x5A}; // DOS / Windows PE executable
    private static final byte[] ELF_HEADER = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46}; // Linux ELF executable
    private static final byte[] SHEBANG_HEADER = new byte[]{(byte) 0x23, (byte) 0x21}; // Shell shebang '#!'

    public void inspectFileContent(Path filePath, String contentType) {
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
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
            return;
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

        // 2. MIME type format validation
        if (contentType != null) {
            String normalizedMime = contentType.toLowerCase().trim();
            if (normalizedMime.equals("application/pdf") && bytesRead >= 4) {
                if (!(header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F')) {
                    throw new InvalidUploadException("Declared MIME type application/pdf does not match file signature (missing %PDF-)");
                }
            } else if (normalizedMime.equals("image/png") && bytesRead >= 8) {
                byte[] pngSig = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
                if (!startsWith(header, bytesRead, pngSig)) {
                    throw new InvalidUploadException("Declared MIME type image/png does not match PNG signature");
                }
            } else if (normalizedMime.equals("image/jpeg") && bytesRead >= 3) {
                if (!((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF)) {
                    throw new InvalidUploadException("Declared MIME type image/jpeg does not match JPEG signature");
                }
            } else if (normalizedMime.equals("image/gif") && bytesRead >= 6) {
                String magic = new String(header, 0, 6);
                if (!magic.equals("GIF87a") && !magic.equals("GIF89a")) {
                    throw new InvalidUploadException("Declared MIME type image/gif does not match GIF signature");
                }
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

