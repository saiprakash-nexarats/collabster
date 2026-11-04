package com.collabsphere_first.messaging.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.messaging.dto.*;
import com.collabsphere_first.messaging.presence.MessagingPresenceService;
import com.collabsphere_first.messaging.service.MessagingAttachmentService;
import com.collabsphere_first.messaging.service.MessagingService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/messages", "/v1/messages"})
@RequiredArgsConstructor
public class MessagingController {

    private final MessagingService messagingService;
    private final CurrentUserService currentUserService;
    private final MessagingAttachmentService attachmentService;
    private final MessagingPresenceService presenceService;

    @PostMapping({"/conversations/direct/{targetUserId}", "/conversations/direct"})
    public ResponseEntity<ApiResponse<ConversationResponse>> getOrCreateDirectConversation(
            @PathVariable(name = "targetUserId", required = false) Long pathTargetUserId,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        Long targetUserId = pathTargetUserId;
        if (targetUserId == null && body != null) {
            Object val = body.get("recipientId") != null ? body.get("recipientId") : body.get("targetUserId");
            if (val != null) {
                targetUserId = Long.valueOf(val.toString());
            }
        }
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId or recipientId is required.");
        }
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConversationResponse response = messagingService.getOrCreateDirectConversation(currentUserId, targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Direct conversation retrieved or created successfully", response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<ConversationPageResponse>> getConversations(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConversationPageResponse response = messagingService.getConversations(currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", response));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable("conversationId") UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ConversationResponse response = messagingService.getConversation(conversationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully", response));
    }

    @GetMapping({"/conversations/{conversationId}/messages", "/conversations/{conversationId}/history"})
    public ResponseEntity<ApiResponse<MessagePageResponse>> getMessages(
            @PathVariable("conversationId") UUID conversationId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MessagePageResponse response = messagingService.getMessages(conversationId, currentUserId, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", response));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable("conversationId") UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MessageResponse response = messagingService.sendMessage(conversationId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent successfully", response));
    }

    @PatchMapping({"/{messageId}", "/conversations/{conversationId}/messages/{messageId}"})
    public ResponseEntity<ApiResponse<MessageResponse>> editMessage(
            @PathVariable("messageId") UUID messageId,
            @PathVariable(name = "conversationId", required = false) UUID conversationId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        MessageResponse response = messagingService.editMessage(messageId, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Message edited successfully", response));
    }

    @DeleteMapping({"/{messageId}", "/conversations/{conversationId}/messages/{messageId}"})
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable("messageId") UUID messageId,
            @PathVariable(name = "conversationId", required = false) UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.deleteMessage(messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    @PutMapping({"/{messageId}/reaction", "/{messageId}/reactions", "/conversations/{conversationId}/messages/{messageId}/reactions", "/conversations/{conversationId}/messages/{messageId}/reaction"})
    public ResponseEntity<ApiResponse<ReactionResponse>> addOrUpdateReaction(
            @PathVariable("messageId") UUID messageId,
            @PathVariable(name = "conversationId", required = false) UUID conversationId,
            @Valid @RequestBody ReactionRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        ReactionResponse response = messagingService.addOrUpdateReaction(messageId, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Reaction updated successfully", response));
    }

    @DeleteMapping({"/{messageId}/reaction", "/{messageId}/reactions", "/conversations/{conversationId}/messages/{messageId}/reactions", "/conversations/{conversationId}/messages/{messageId}/reaction"})
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            @PathVariable("messageId") UUID messageId,
            @PathVariable(name = "conversationId", required = false) UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.removeReaction(messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully", null));
    }

    @PostMapping("/{messageId}/delivered")
    public ResponseEntity<ApiResponse<Void>> markDelivered(
            @PathVariable("messageId") UUID messageId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.markDelivered(messageId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Message delivery acknowledged", null));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationRead(
            @PathVariable("conversationId") UUID conversationId,
            @Valid @RequestBody ReadReceiptRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.markConversationRead(conversationId, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Conversation marked read", null));
    }

    @PutMapping("/conversations/{conversationId}/mute")
    public ResponseEntity<ApiResponse<Void>> muteConversation(
            @PathVariable("conversationId") UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.muteConversation(conversationId, currentUserId, true);
        return ResponseEntity.ok(ApiResponse.success("Conversation muted successfully", null));
    }

    @DeleteMapping("/conversations/{conversationId}/mute")
    public ResponseEntity<ApiResponse<Void>> unmuteConversation(
            @PathVariable("conversationId") UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.muteConversation(conversationId, currentUserId, false);
        return ResponseEntity.ok(ApiResponse.success("Conversation unmuted successfully", null));
    }

    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(
            @PathVariable("conversationId") UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.archiveConversation(conversationId, currentUserId, true);
        return ResponseEntity.ok(ApiResponse.success("Conversation archived successfully", null));
    }

    @DeleteMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<ApiResponse<Void>> unarchiveConversation(
            @PathVariable("conversationId") UUID conversationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        messagingService.archiveConversation(conversationId, currentUserId, false);
        return ResponseEntity.ok(ApiResponse.success("Conversation unarchived successfully", null));
    }

    @GetMapping("/conversations/{conversationId}/attachments/{attachmentId}/url")
    public ResponseEntity<ApiResponse<AttachmentSignedUrlResponse>> getAttachmentSignedUrl(
            @PathVariable("conversationId") UUID conversationId,
            @PathVariable("attachmentId") UUID attachmentId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        AttachmentSignedUrlResponse response = messagingService.getAttachmentSignedUrl(conversationId, attachmentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Attachment signed URL generated successfully", response));
    }

    @PostMapping("/media/register")
    public ResponseEntity<ApiResponse<UploadedMediaResponse>> registerUploadedMedia(
            @Valid @RequestBody RegisterMediaRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        UploadedMediaResponse response = attachmentService.registerUploadedMedia(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media asset registered successfully", response));
    }

    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadedMediaResponse>> uploadMedia(
            @RequestParam("file") MultipartFile file
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        UploadedMediaResponse response = attachmentService.uploadMedia(file, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media asset uploaded successfully", response));
    }

    @PostMapping("/presence/heartbeat")
    public ResponseEntity<ApiResponse<Void>> presenceHeartbeat(
            @RequestParam(name = "sessionId", required = false, defaultValue = "default") String sessionId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        presenceService.onHeartbeat(currentUserId, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat processed", null));
    }
}
