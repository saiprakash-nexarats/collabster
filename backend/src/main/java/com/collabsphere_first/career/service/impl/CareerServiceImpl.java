package com.collabsphere_first.career.service.impl;

import com.collabsphere_first.career.dto.*;
import com.collabsphere_first.career.entity.CareerApplication;
import com.collabsphere_first.career.entity.CareerJob;
import com.collabsphere_first.career.repository.CareerApplicationRepository;
import com.collabsphere_first.career.repository.CareerJobRepository;
import com.collabsphere_first.career.service.CareerService;
import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.collabsphere_first.storage.dto.StorageFileResource;
import com.collabsphere_first.storage.entity.StorageObject;
import com.collabsphere_first.storage.enums.UploadState;
import com.collabsphere_first.storage.provider.ObjectStorageProvider;
import com.collabsphere_first.storage.repository.StorageObjectRepository;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareerServiceImpl implements CareerService {

    private final CareerJobRepository jobRepository;
    private final CareerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final StorageObjectRepository storageObjectRepository;
    private final ObjectStorageProvider storageProvider;

    @Override
    @Transactional(readOnly = true)
    public List<CareerJobDto> listJobs() {
        return jobRepository.findByStateOrderByCreatedAtDesc("published")
                .stream()
                .map(this::mapJobToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CareerJobDto createJob(CreateJobRequest request, Long employerId) {
        CareerJob job = CareerJob.builder()
                .employerId(employerId)
                .companyName(request.getCompanyName().trim())
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .location(request.getLocation() != null ? request.getLocation().trim() : "Flexible")
                .workMode(request.getWorkMode() != null ? request.getWorkMode().trim() : "Flexible")
                .employmentType(request.getEmploymentType() != null ? request.getEmploymentType().trim() : "Full-time")
                .salaryRange(request.getSalaryRange())
                .state("published")
                .build();
        CareerJob saved = jobRepository.save(job);
        log.info("Created career job {} by employer {}", saved.getId(), employerId);
        return mapJobToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CareerJobDto getJobById(UUID jobId) {
        CareerJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id " + jobId));
        return mapJobToDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareerApplicationDto> listApplicantApplications(Long applicantId) {
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicantId)
                .stream()
                .map(this::mapApplicationToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CareerApplicationDto apply(UUID jobId, ApplyJobRequest request, Long applicantId) {
        CareerJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id " + jobId));

        if (!"published".equalsIgnoreCase(job.getState())) {
            throw new IllegalStateException("This job is no longer accepting applications");
        }

        if (applicationRepository.findByJobIdAndApplicantId(jobId, applicantId).isPresent()) {
            throw new IllegalStateException("You have already applied for this position");
        }

        UUID resumeId = (request != null) ? request.getResumeObjectId() : null;
        if (resumeId != null) {
            StorageObject resumeObj = storageObjectRepository.findByIdAndIsDeletedFalse(resumeId)
                    .orElseThrow(() -> new IllegalArgumentException("Resume file not found or deleted: " + resumeId));

            if (!applicantId.equals(resumeObj.getOwnerId())) {
                throw new AccessDeniedException("Applicant does not own this resume object");
            }

            if (resumeObj.getUploadState() != UploadState.COMPLETED) {
                throw new IllegalStateException("Resume file upload has not been completed");
            }

            if (Boolean.TRUE.equals(resumeObj.getIsDeleted())) {
                throw new IllegalArgumentException("Resume file has been deleted");
            }

            String contentType = resumeObj.getContentType();
            String filename = resumeObj.getFilename() != null ? resumeObj.getFilename().toLowerCase() : "";
            boolean isAllowedMime = contentType != null && (
                    contentType.equalsIgnoreCase("application/pdf") ||
                    contentType.equalsIgnoreCase("application/msword") ||
                    contentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                    (contentType.equalsIgnoreCase("application/octet-stream") && (filename.endsWith(".pdf") || filename.endsWith(".docx") || filename.endsWith(".doc")))
            );
            boolean isAllowedExt = filename.endsWith(".pdf") || filename.endsWith(".doc") || filename.endsWith(".docx");
            if (!isAllowedMime && !isAllowedExt) {
                throw new IllegalArgumentException("Invalid resume format. Only PDF and Word documents are permitted.");
            }
        }

        CareerApplication application = CareerApplication.builder()
                .jobId(jobId)
                .applicantId(applicantId)
                .resumeObjectId(resumeId)
                .note(request != null ? request.getNote() : null)
                .state("submitted")
                .build();

        CareerApplication saved = applicationRepository.save(application);
        log.info("Applicant {} applied for job {} with application {}", applicantId, jobId, saved.getId());
        return mapApplicationToDto(saved);
    }

    @Override
    @Transactional
    public CareerApplicationDto withdraw(UUID applicationId, Long applicantId) {
        CareerApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id " + applicationId));

        if (!app.getApplicantId().equals(applicantId)) {
            throw new AccessDeniedException("You are not authorized to withdraw this application");
        }

        app.setState("withdrawn");
        CareerApplication saved = applicationRepository.save(app);
        log.info("Applicant {} withdrew application {}", applicantId, applicationId);
        return mapApplicationToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CareerApplicationDto> listJobApplications(UUID jobId, Long currentUserId) {
        CareerJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id " + jobId));

        if (!job.getEmployerId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the employer can view applications for this job");
        }

        List<CareerApplication> apps = applicationRepository.findByJobIdOrderByCreatedAtDesc(jobId);
        List<Long> applicantIds = apps.stream().map(CareerApplication::getApplicantId).distinct().toList();
        Map<Long, User> userMap = userRepository.findAllById(applicantIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return apps.stream()
                .map(a -> mapApplicationToDtoWithUser(a, userMap.get(a.getApplicantId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CareerApplicationDto reviewApplication(UUID jobId, UUID applicationId, ReviewApplicationRequest request,
            Long currentUserId) {
        CareerJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id " + jobId));

        if (!job.getEmployerId().equals(currentUserId)) {
            throw new AccessDeniedException("Only the employer can review applications for this job");
        }

        CareerApplication app = applicationRepository.findByIdAndJobId(applicationId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found with id " + applicationId + " for job " + jobId));

        app.setState(request.getStatus().toLowerCase());
        if (request.getReviewNotes() != null) {
            app.setReviewNotes(request.getReviewNotes());
        }

        CareerApplication saved = applicationRepository.save(app);
        log.info("Employer {} updated application {} to state {}", currentUserId, applicationId, saved.getState());
        User applicant = userRepository.findById(app.getApplicantId()).orElse(null);
        return mapApplicationToDtoWithUser(saved, applicant);
    }

    @Override
    @Transactional(readOnly = true)
    public StorageFileResource downloadResume(UUID applicationId, Long currentUserId) {
        CareerApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id " + applicationId));

        CareerJob job = jobRepository.findById(app.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id " + app.getJobId()));

        boolean isEmployer = job.getEmployerId().equals(currentUserId);
        boolean isApplicant = app.getApplicantId().equals(currentUserId);
        if (!isEmployer && !isApplicant) {
            throw new AccessDeniedException("Only the employer or the applicant can download this resume");
        }

        if (app.getResumeObjectId() == null) {
            throw new ResourceNotFoundException("No resume attached to this application");
        }

        StorageObject storageObject = storageObjectRepository.findByIdAndIsDeletedFalse(app.getResumeObjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Resume storage object not found or deleted"));

        if (storageObject.getUploadState() != UploadState.COMPLETED) {
            throw new IllegalStateException("Resume file upload has not completed: " + storageObject.getUploadState());
        }

        InputStream stream = storageProvider.retrieveObject(storageObject.getStorageKey());
        return StorageFileResource.builder()
                .resource(new InputStreamResource(stream))
                .filename(storageObject.getFilename())
                .contentType(storageObject.getContentType() != null ? storageObject.getContentType() : "application/pdf")
                .contentLength(storageObject.getVerifiedBytes() != null ? storageObject.getVerifiedBytes() : storageObject.getExpectedBytes())
                .build();
    }

    private CareerJobDto mapJobToDto(CareerJob job) {
        return CareerJobDto.builder()
                .id(job.getId())
                .employerId(job.getEmployerId())
                .companyName(job.getCompanyName())
                .title(job.getTitle())
                .description(job.getDescription())
                .location(job.getLocation())
                .workMode(job.getWorkMode())
                .employmentType(job.getEmploymentType())
                .salaryRange(job.getSalaryRange())
                .state(job.getState())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private CareerApplicationDto mapApplicationToDto(CareerApplication app) {
        User applicant = userRepository.findById(app.getApplicantId()).orElse(null);
        return mapApplicationToDtoWithUser(app, applicant);
    }

    private CareerApplicationDto mapApplicationToDtoWithUser(CareerApplication app, User applicant) {
        String name = applicant != null ? (applicant.getFirstName() + " " + applicant.getLastName()).trim() : null;
        String email = applicant != null ? applicant.getEmail() : null;
        return CareerApplicationDto.builder()
                .id(app.getId())
                .jobId(app.getJobId())
                .applicantId(app.getApplicantId())
                .applicantName(name)
                .applicantEmail(email)
                .resumeObjectId(app.getResumeObjectId())
                .note(app.getNote())
                .state(app.getState())
                .reviewNotes(app.getReviewNotes())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
