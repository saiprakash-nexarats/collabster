package com.collabsphere_first.career;

import com.collabsphere_first.career.dto.*;
import com.collabsphere_first.career.entity.CareerApplication;
import com.collabsphere_first.career.entity.CareerJob;
import com.collabsphere_first.career.repository.CareerApplicationRepository;
import com.collabsphere_first.career.repository.CareerJobRepository;
import com.collabsphere_first.career.service.impl.CareerServiceImpl;
import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerJobRepository jobRepository;
    @Mock
    private CareerApplicationRepository applicationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CareerServiceImpl careerService;

    private Long employerId;
    private Long applicantId;
    private UUID jobId;
    private CareerJob job;

    @BeforeEach
    void setUp() {
        employerId = 401L;
        applicantId = 501L;
        jobId = UUID.randomUUID();
        job = CareerJob.builder()
                .id(jobId)
                .employerId(employerId)
                .companyName("Collobster Cloud")
                .title("Staff Infrastructure Engineer")
                .description("Build globally distributed database platforms")
                .location("Bengaluru / Remote")
                .workMode("Hybrid")
                .employmentType("Full-time")
                .salaryRange("$120k - $160k")
                .state("published")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Create job stores published job record")
    void testCreateJob() {
        CreateJobRequest request = CreateJobRequest.builder()
                .companyName("Acme Corp")
                .title("Senior Backend Engineer")
                .description("Spring Boot microservices")
                .location("Remote")
                .workMode("Remote")
                .employmentType("Full-time")
                .salaryRange("$100k - $140k")
                .build();

        when(jobRepository.save(any(CareerJob.class))).thenAnswer(i -> {
            CareerJob j = i.getArgument(0);
            j.setId(UUID.randomUUID());
            return j;
        });

        CareerJobDto created = careerService.createJob(request, employerId);
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Senior Backend Engineer");
        assertThat(created.getState()).isEqualTo("published");
        verify(jobRepository).save(any(CareerJob.class));
    }

    @Test
    @DisplayName("Apply for job succeeds and rejects duplicate applications")
    void testApplySuccessAndDuplicatePrevention() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(applicationRepository.findByJobIdAndApplicantId(jobId, applicantId)).thenReturn(Optional.empty());

        when(applicationRepository.save(any(CareerApplication.class))).thenAnswer(i -> {
            CareerApplication a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        ApplyJobRequest request = ApplyJobRequest.builder().note("Experienced engineer").build();
        CareerApplicationDto app = careerService.apply(jobId, request, applicantId);
        assertThat(app).isNotNull();
        assertThat(app.getState()).isEqualTo("submitted");

        // Duplicate application is blocked
        when(applicationRepository.findByJobIdAndApplicantId(jobId, applicantId))
                .thenReturn(Optional.of(CareerApplication.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> careerService.apply(jobId, request, applicantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You have already applied for this position");
    }

    @Test
    @DisplayName("Withdraw application allows only the applicant to withdraw")
    void testWithdrawApplicationAuthorization() {
        UUID appId = UUID.randomUUID();
        CareerApplication app = CareerApplication.builder()
                .id(appId)
                .jobId(jobId)
                .applicantId(applicantId)
                .state("submitted")
                .build();

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(app));

        // Unauthorized user cannot withdraw
        Long otherUser = 999L;
        assertThatThrownBy(() -> careerService.withdraw(appId, otherUser))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not authorized to withdraw this application");

        // Applicant can withdraw
        when(applicationRepository.save(any(CareerApplication.class))).thenAnswer(i -> i.getArgument(0));
        CareerApplicationDto withdrawn = careerService.withdraw(appId, applicantId);
        assertThat(withdrawn.getState()).isEqualTo("withdrawn");
    }

    @Test
    @DisplayName("List and review applications restricted to employer only")
    void testEmployerReviewAuthorization() {
        UUID appId = UUID.randomUUID();
        CareerApplication app = CareerApplication.builder()
                .id(appId)
                .jobId(jobId)
                .applicantId(applicantId)
                .state("submitted")
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Long nonEmployer = 888L;
        // Non-employer cannot list applications for this job
        assertThatThrownBy(() -> careerService.listJobApplications(jobId, nonEmployer))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the employer can view applications for this job");

        // Non-employer cannot review applications
        ReviewApplicationRequest reviewReq = ReviewApplicationRequest.builder()
                .status("shortlisted")
                .reviewNotes("Strong candidate profile")
                .build();

        assertThatThrownBy(() -> careerService.reviewApplication(jobId, appId, reviewReq, nonEmployer))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the employer can review applications for this job");

        // Employer can review
        when(applicationRepository.findByIdAndJobId(appId, jobId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(CareerApplication.class))).thenAnswer(i -> i.getArgument(0));

        CareerApplicationDto reviewed = careerService.reviewApplication(jobId, appId, reviewReq, employerId);
        assertThat(reviewed.getState()).isEqualTo("shortlisted");
        assertThat(reviewed.getReviewNotes()).isEqualTo("Strong candidate profile");
    }
}
