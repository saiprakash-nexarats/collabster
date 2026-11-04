package com.collabsphere_first.storage.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.dto.StorageObjectResponse;
import com.collabsphere_first.storage.dto.UploadInitiationRequest;
import com.collabsphere_first.storage.dto.UploadTicketResponse;
import com.collabsphere_first.storage.service.StorageManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/storage", "/api/v1/storage"})
@RequiredArgsConstructor
public class StorageController {

    private final StorageManagementService storageManagementService;
    private final CurrentUserService currentUserService;

    @PostMapping("/uploads")
    public ResponseEntity<ApiResponse<UploadTicketResponse>> requestUpload(
            @Valid @RequestBody UploadInitiationRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        UploadTicketResponse response = storageManagementService.requestUpload(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upload session created successfully", response));
    }

    @PutMapping(value = "/raw/{id}")
    public ResponseEntity<Void> directUpload(
            @PathVariable UUID id,
            @RequestParam("expires") long expires,
            @RequestParam("signature") String signature,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            @RequestHeader(value = HttpHeaders.CONTENT_LENGTH, required = false) Long contentLength,
            HttpServletRequest request
    ) throws IOException {
        storageManagementService.handleDirectUpload(id, request.getInputStream(), contentType, contentLength, expires, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/uploads/{id}/complete")
    public ResponseEntity<ApiResponse<StorageObjectResponse>> completeUpload(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        StorageObjectResponse response = storageManagementService.completeUpload(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Upload completed and verified successfully", response));
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<ApiResponse<StorageObjectResponse>> getFileMetadata(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        StorageObjectResponse response = storageManagementService.getMetadata(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        StorageFileResource fileResource = storageManagementService.downloadFile(id, currentUserId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileResource.getContentType()))
                .contentLength(fileResource.getContentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource.getResource());
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        storageManagementService.deleteFile(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}

