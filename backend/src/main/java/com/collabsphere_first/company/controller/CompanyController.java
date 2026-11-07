package com.collabsphere_first.company.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.company.dto.*;
import com.collabsphere_first.company.service.CompanyService;
import com.collabsphere_first.security.service.CurrentUserService;
import com.collabsphere_first.storage.dto.StorageFileResource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/companies", "/api/v1/companies"})
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyMembershipRowDto>>> listCompanies() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CompanyMembershipRowDto> companies = companyService.listUserCompanies(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(companies));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyDto>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyDto company = companyService.createCompany(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(company));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<CompanyMemberDto>> inviteMember(
            @PathVariable UUID id,
            @RequestBody InviteCompanyMemberRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyMemberDto member = companyService.inviteMember(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(member));
    }

    @PostMapping("/{id}/membership/accept")
    public ResponseEntity<ApiResponse<CompanyMemberDto>> acceptInvitation(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyMemberDto member = companyService.acceptInvitation(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(member));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<CompanyTaskDto>>> listTasks(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CompanyTaskDto> tasks = companyService.listTasks(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<CompanyTaskDto>> createTask(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCompanyTaskRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyTaskDto task = companyService.createTask(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(task));
    }

    @PatchMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<CompanyTaskDto>> updateTask(
            @PathVariable UUID id,
            @PathVariable UUID taskId,
            @RequestBody UpdateCompanyTaskRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyTaskDto task = companyService.updateTask(id, taskId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @GetMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<List<CompanyDocumentDto>>> listDocuments(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CompanyDocumentDto> documents = companyService.listDocuments(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/{id}/documents")
    public ResponseEntity<ApiResponse<CompanyDocumentDto>> createDocument(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCompanyDocumentRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyDocumentDto document = companyService.createDocument(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(document));
    }

    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<ApiResponse<CompanyDownloadTicketDto>> downloadDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CompanyDownloadTicketDto ticket = companyService.downloadDocument(id, documentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping(value = {"/{id}/documents/{documentId}/raw", "/{id}/documents/{documentId}/file"})
    public ResponseEntity<Resource> downloadDocumentFile(
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        StorageFileResource fileResource = companyService.downloadDocumentFile(id, documentId, currentUserId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(fileResource.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileResource.getContentLength()))
                .body(fileResource.getResource());
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> archiveDocument(
            @PathVariable UUID id,
            @PathVariable UUID documentId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        companyService.archiveDocument(id, documentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Document archived successfully", null));
    }
}
