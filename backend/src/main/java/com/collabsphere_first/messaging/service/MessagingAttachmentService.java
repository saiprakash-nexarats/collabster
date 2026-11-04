package com.collabsphere_first.messaging.service;

import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.entity.DirectMessage;
import com.collabsphere_first.messaging.entity.MessageAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessagingAttachmentService {

    void validateAttachments(List<AttachmentRequest> attachments);

    void validateAttachments(List<AttachmentRequest> attachments, Long currentUserId);

    UploadedMediaResponse registerUploadedMedia(RegisterMediaRequest request, Long currentUserId);

    UploadedMediaResponse uploadMedia(MultipartFile file, Long currentUserId);

    List<MessageAttachment> createAttachments(DirectMessage message, List<AttachmentRequest> requests);

    AttachmentResponse toResponse(MessageAttachment attachment);
}
