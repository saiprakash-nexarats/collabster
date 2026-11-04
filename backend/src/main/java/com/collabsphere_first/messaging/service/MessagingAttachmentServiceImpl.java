package com.collabsphere_first.messaging.service;

import com.collabsphere_first.integration.storage.StorageService;
import com.collabsphere_first.messaging.config.MessagingProperties;
import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.DirectMessage;
import com.collabsphere_first.messaging.entity.MessageAttachment;
import com.collabsphere_first.messaging.entity.UploadedMedia;
import com.collabsphere_first.messaging.enums.AttachmentMediaType;
import com.collabsphere_first.messaging.exception.InvalidAttachmentException;
import com.collabsphere_first.messaging.exception.MessagingAccessDeniedException;
import com.collabsphere_first.messaging.repository.UploadedMediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingAttachmentServiceImpl implements MessagingAttachmentService {

    private static final Pattern SAFE_STORAGE_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-/]+\\.[a-zA-Z0-9]+$");

    private final MessagingProperties messagingProperties;
    private final ObjectProvider<StorageService> storageServiceProvider;
    private final UploadedMediaRepository uploadedMediaRepository;

    @Override
    public void validateAttachments(List<AttachmentRequest> attachments) {
        validateAttachments(attachments, null);
    }

    @Override
    public void validateAttachments(List<AttachmentRequest> attachments, Long currentUserId) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        if (attachments.size() > messagingProperties.getMaxAttachmentsPerMessage()) {
            throw new InvalidAttachmentException("Too many attachments. Maximum allowed is " +
                    messagingProperties.getMaxAttachmentsPerMessage());
        }

        for (AttachmentRequest req : attachments) {
            if (req.mediaType() == null) {
                throw new InvalidAttachmentException("mediaType is required for attachment");
            }
            if (req.storageKey() == null || req.storageKey().isBlank()) {
                throw new InvalidAttachmentException("storageKey is required for attachment");
            }
            validateStorageKey(req.storageKey());

            if (req.mimeType() == null || req.mimeType().isBlank()) {
                throw new InvalidAttachmentException("mimeType is required for attachment");
            }
            String normalizedMime = req.mimeType().trim().toLowerCase();
            if (!messagingProperties.getAllowedMimeTypes().contains(normalizedMime)) {
                throw new InvalidAttachmentException("MIME type " + req.mimeType() + " is not permitted.");
            }

            if (req.fileSize() <= 0) {
                throw new InvalidAttachmentException("fileSize must be greater than zero");
            }
            if (req.fileSize() > messagingProperties.getMaxAttachmentFileSize()) {
                throw new InvalidAttachmentException("Attachment file size " + req.fileSize() +
                        " bytes exceeds maximum allowed " + messagingProperties.getMaxAttachmentFileSize() + " bytes");
            }

            if (currentUserId != null) {
                String normalizedKey = req.storageKey().replace('\\', '/').trim();
                UploadedMedia media = uploadedMediaRepository.findByStorageKey(normalizedKey)
                        .orElseThrow(() -> new MessagingAccessDeniedException("Media asset not found or not owned by user: " + req.storageKey()));
                if (!media.getOwnerUserId().equals(currentUserId)) {
                    throw new MessagingAccessDeniedException("User is not authorized to attach asset owned by another user: " + req.storageKey());
                }
            }
        }
    }

    private void validateStorageKey(String storageKey) {
        String normalized = storageKey.replace('\\', '/').trim();
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.contains(":") ||
                !SAFE_STORAGE_KEY_PATTERN.matcher(normalized).matches()) {
            throw new InvalidAttachmentException("Invalid storage key format: path traversal, absolute paths, or special characters are forbidden.");
        }
    }

    @Override
    public UploadedMediaResponse registerUploadedMedia(RegisterMediaRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new MessagingAccessDeniedException("Authenticated user required to register uploaded media");
        }
        if (request == null) {
            throw new InvalidAttachmentException("Registration request cannot be null");
        }
        validateStorageKey(request.storageKey());

        String normalizedMime = request.mimeType().trim().toLowerCase();
        if (!messagingProperties.getAllowedMimeTypes().contains(normalizedMime)) {
            throw new InvalidAttachmentException("MIME type " + request.mimeType() + " is not permitted.");
        }
        if (request.fileSize() <= 0) {
            throw new InvalidAttachmentException("fileSize must be greater than zero");
        }
        if (request.fileSize() > messagingProperties.getMaxAttachmentFileSize()) {
            throw new InvalidAttachmentException("Attachment file size " + request.fileSize() +
                    " bytes exceeds maximum allowed " + messagingProperties.getMaxAttachmentFileSize() + " bytes");
        }

        String normalizedKey = request.storageKey().replace('\\', '/').trim();
        UploadedMedia media = uploadedMediaRepository.findByStorageKey(normalizedKey)
                .orElseGet(() -> UploadedMedia.builder()
                        .ownerUserId(currentUserId)
                        .storageKey(normalizedKey)
                        .mediaType(request.mediaType())
                        .mimeType(normalizedMime)
                        .fileName(request.fileName() != null ? request.fileName().trim() : "file")
                        .fileSize(request.fileSize())
                        .uploadStatus("COMPLETED")
                        .createdAt(Instant.now())
                        .build());

        if (!media.getOwnerUserId().equals(currentUserId)) {
            throw new MessagingAccessDeniedException("Storage key is already registered to another user");
        }

        media = uploadedMediaRepository.save(media);
        return new UploadedMediaResponse(
                media.getId(),
                media.getOwnerUserId(),
                media.getStorageKey(),
                media.getMediaType(),
                media.getMimeType(),
                media.getFileName(),
                media.getFileSize(),
                media.getUploadStatus(),
                media.getCreatedAt()
        );
    }

    @Override
    public UploadedMediaResponse uploadMedia(MultipartFile file, Long currentUserId) {
        if (currentUserId == null) {
            throw new MessagingAccessDeniedException("Authenticated user required to upload media");
        }
        if (file == null || file.isEmpty()) {
            throw new InvalidAttachmentException("Uploaded file cannot be empty");
        }

        long size = file.getSize();
        if (size > messagingProperties.getMaxAttachmentFileSize()) {
            throw new InvalidAttachmentException("File size exceeds maximum allowed limit");
        }

        String contentType = file.getContentType() != null ? file.getContentType().trim().toLowerCase() : "application/octet-stream";
        if (!messagingProperties.getAllowedMimeTypes().contains(contentType)) {
            throw new InvalidAttachmentException("MIME type " + contentType + " is not permitted.");
        }

        StorageService storageService = storageServiceProvider.getIfAvailable();
        if (storageService == null) {
            throw new IllegalStateException("Storage service is not available");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String sanitizedName = storageService.sanitizeFilename(originalFilename);
        String destinationKey = "messages/" + currentUserId + "/" + UUID.randomUUID() + "-" + sanitizedName;

        try {
            storageService.uploadFile(destinationKey, file.getBytes(), contentType);
        } catch (Exception ex) {
            log.error("Failed to store uploaded file: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Could not store uploaded file: " + ex.getMessage());
        }

        AttachmentMediaType mediaType = inferMediaType(contentType);
        UploadedMedia media = UploadedMedia.builder()
                .ownerUserId(currentUserId)
                .storageKey(destinationKey)
                .mediaType(mediaType)
                .mimeType(contentType)
                .fileName(sanitizedName)
                .fileSize(size)
                .uploadStatus("COMPLETED")
                .createdAt(Instant.now())
                .build();
        media = uploadedMediaRepository.save(media);

        return new UploadedMediaResponse(
                media.getId(),
                media.getOwnerUserId(),
                media.getStorageKey(),
                media.getMediaType(),
                media.getMimeType(),
                media.getFileName(),
                media.getFileSize(),
                media.getUploadStatus(),
                media.getCreatedAt()
        );
    }

    private AttachmentMediaType inferMediaType(String contentType) {
        if (contentType.startsWith("image/")) {
            return AttachmentMediaType.IMAGE;
        } else if (contentType.startsWith("video/")) {
            return AttachmentMediaType.VIDEO;
        } else if (contentType.startsWith("audio/")) {
            return AttachmentMediaType.AUDIO;
        } else {
            return AttachmentMediaType.DOCUMENT;
        }
    }

    @Override
    public List<MessageAttachment> createAttachments(DirectMessage message, List<AttachmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<MessageAttachment> attachments = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            AttachmentRequest req = requests.get(i);
            MessageAttachment attachment = MessageAttachment.builder()
                    .messageId(message.getId())
                    .mediaType(req.mediaType())
                    .storageKey(req.storageKey().replace('\\', '/').trim())
                    .mimeType(req.mimeType().trim().toLowerCase())
                    .fileName(req.fileName() != null ? req.fileName().trim() : "file")
                    .fileSize(req.fileSize())
                    .width(req.width())
                    .height(req.height())
                    .durationMs(req.durationMs())
                    .displayOrder(i)
                    .createdAt(Instant.now())
                    .build();
            attachments.add(attachment);
        }
        return attachments;
    }

    @Override
    public AttachmentResponse toResponse(MessageAttachment attachment) {
        if (attachment == null) {
            return null;
        }

        String signedUrl = null;
        StorageService storageService = storageServiceProvider.getIfAvailable();
        if (storageService != null && attachment.getStorageKey() != null) {
            try {
                signedUrl = storageService.generateSignedUrl(attachment.getStorageKey(), 60);
            } catch (Exception ex) {
                log.warn("Failed to generate signed URL for attachment {}: {}", attachment.getId(), ex.getMessage());
            }
        }

        return new AttachmentResponse(
                attachment.getId(),
                attachment.getMediaType(),
                attachment.getStorageKey(),
                signedUrl,
                attachment.getMimeType(),
                attachment.getFileName(),
                attachment.getFileSize(),
                attachment.getWidth(),
                attachment.getHeight(),
                attachment.getDurationMs(),
                attachment.getDisplayOrder(),
                attachment.getCreatedAt()
        );
    }
}
