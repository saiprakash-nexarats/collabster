package com.collabsphere_first.career.controller;

import com.collabsphere_first.career.dto.*;
import com.collabsphere_first.career.service.CareerService;
import com.collabsphere_first.community.dto.ApiResponse;
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
@RequestMapping({"/v1/career", "/api/v1/career"})
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;
    private final CurrentUserService currentUserService;

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<CareerJobDto>>> listJobs() {
        List<CareerJobDto> jobs = careerService.listJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @PostMapping("/jobs")
    public ResponseEntity<ApiResponse<CareerJobDto>> createJob(
            @Valid @RequestBody CreateJobRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CareerJobDto job = careerService.createJob(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(job));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<List<CareerApplicationDto>>> listApplicantApplications() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CareerApplicationDto> applications = careerService.listApplicantApplications(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @PostMapping("/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<CareerApplicationDto>> apply(
            @PathVariable UUID jobId,
            @RequestBody(required = false) ApplyJobRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CareerApplicationDto application = careerService.apply(jobId, request != null ? request : new ApplyJobRequest(), currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(application));
    }

    @PostMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<ApiResponse<CareerApplicationDto>> withdraw(
            @PathVariable UUID applicationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CareerApplicationDto application = careerService.withdraw(applicationId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(application));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<List<CareerApplicationDto>>> listJobApplications(
            @PathVariable UUID jobId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CareerApplicationDto> applications = careerService.listJobApplications(jobId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @PatchMapping("/jobs/{jobId}/applications/{applicationId}/review")
    public ResponseEntity<ApiResponse<CareerApplicationDto>> reviewApplication(
            @PathVariable UUID jobId,
            @PathVariable UUID applicationId,
            @Valid @RequestBody ReviewApplicationRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CareerApplicationDto application = careerService.reviewApplication(jobId, applicationId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(application));
    }

    @GetMapping({"/applications/{applicationId}/resume", "/jobs/{jobId}/applications/{applicationId}/resume"})
    public ResponseEntity<Resource> downloadResume(
            @PathVariable UUID applicationId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        StorageFileResource fileResource = careerService.downloadResume(applicationId, currentUserId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileResource.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                .body(fileResource.getResource());
    }
}

