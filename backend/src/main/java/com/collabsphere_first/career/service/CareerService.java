package com.collabsphere_first.career.service;

import com.collabsphere_first.career.dto.*;
import com.collabsphere_first.storage.dto.StorageFileResource;

import java.util.List;
import java.util.UUID;

public interface CareerService {
    List<CareerJobDto> listJobs();
    CareerJobDto createJob(CreateJobRequest request, Long employerId);
    CareerJobDto getJobById(UUID jobId);
    List<CareerApplicationDto> listApplicantApplications(Long applicantId);
    CareerApplicationDto apply(UUID jobId, ApplyJobRequest request, Long applicantId);
    CareerApplicationDto withdraw(UUID applicationId, Long applicantId);
    List<CareerApplicationDto> listJobApplications(UUID jobId, Long currentUserId);
    CareerApplicationDto reviewApplication(UUID jobId, UUID applicationId, ReviewApplicationRequest request, Long currentUserId);
    StorageFileResource downloadResume(UUID applicationId, Long currentUserId);
}

