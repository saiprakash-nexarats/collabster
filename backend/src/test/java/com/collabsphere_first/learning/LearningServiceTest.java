package com.collabsphere_first.learning;

import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.learning.dto.*;
import com.collabsphere_first.learning.entity.*;
import com.collabsphere_first.learning.repository.*;
import com.collabsphere_first.learning.service.impl.LearningServiceImpl;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseLessonRepository courseLessonRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private LessonCompletionRepository lessonCompletionRepository;
    @Mock
    private CourseCertificateRepository courseCertificateRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LearningServiceImpl learningService;

    private Long userId;
    private UUID courseId;
    private Course course;

    @BeforeEach
    void setUp() {
        userId = 101L;
        courseId = UUID.randomUUID();
        course = Course.builder()
                .id(courseId)
                .instructorId(userId)
                .title("Full Stack Distributed Systems")
                .description("Production architectures with Spring and React")
                .category("Engineering")
                .difficulty("advanced")
                .state("published")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Create course stores published course entity")
    void testCreateCourse() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .title("Distributed Systems")
                .description("Architecture course")
                .category("Cloud")
                .difficulty("intermediate")
                .build();

        when(courseRepository.save(any(Course.class))).thenAnswer(i -> {
            Course c = i.getArgument(0);
            c.setId(courseId);
            return c;
        });

        CourseDto created = learningService.createCourse(request, userId);
        assertThat(created).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Distributed Systems");
        assertThat(created.getState()).isEqualTo("published");
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    @DisplayName("Enroll registers user in course when not previously enrolled")
    void testEnrollSuccess() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(courseId, userId)).thenReturn(Optional.empty());
        when(courseEnrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(i -> i.getArgument(0));

        CourseEnrollment enrollment = learningService.enroll(courseId, userId);
        assertThat(enrollment.getEnrollmentState()).isEqualTo("active");
        verify(courseEnrollmentRepository).save(any(CourseEnrollment.class));
    }

    @Test
    @DisplayName("Complete lesson is idempotent and creates record once")
    void testCompleteLessonIdempotent() {
        UUID lessonId = UUID.randomUUID();
        CourseLesson lesson = CourseLesson.builder().id(lessonId).courseId(courseId).title("Intro").build();
        when(courseLessonRepository.findByCourseIdAndId(courseId, lessonId)).thenReturn(Optional.of(lesson));
        when(courseEnrollmentRepository.findByCourseIdAndUserId(courseId, userId)).thenReturn(Optional.empty());
        when(courseEnrollmentRepository.save(any(CourseEnrollment.class))).thenAnswer(i -> i.getArgument(0));
        when(lessonCompletionRepository.existsByLessonIdAndUserId(lessonId, userId)).thenReturn(false);
        when(courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(lesson));
        when(lessonCompletionRepository.countByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(1L);

        learningService.completeLesson(courseId, lessonId, userId);
        verify(lessonCompletionRepository, times(1)).save(any(LessonCompletion.class));

        // When already completed, saving is not repeated
        when(lessonCompletionRepository.existsByLessonIdAndUserId(lessonId, userId)).thenReturn(true);
        learningService.completeLesson(courseId, lessonId, userId);
        verify(lessonCompletionRepository, times(1)).save(any(LessonCompletion.class));
    }

    @Test
    @DisplayName("Claim certificate rejects claim if not all lessons completed")
    void testClaimCertificateIncompleteThrows() {
        CourseLesson l1 = CourseLesson.builder().id(UUID.randomUUID()).courseId(courseId).build();
        CourseLesson l2 = CourseLesson.builder().id(UUID.randomUUID()).courseId(courseId).build();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(l1, l2));
        when(lessonCompletionRepository.countByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(1L);

        assertThatThrownBy(() -> learningService.claimCertificate(courseId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All lessons must be completed");
    }

    @Test
    @DisplayName("Claim certificate issues certificate with SHA-256 verification hash when complete")
    void testClaimCertificateSuccess() {
        CourseLesson l1 = CourseLesson.builder().id(UUID.randomUUID()).courseId(courseId).build();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)).thenReturn(List.of(l1));
        when(lessonCompletionRepository.countByUserIdAndLessonIdIn(eq(userId), anyList())).thenReturn(1L);
        when(courseCertificateRepository.findByCourseIdAndUserId(courseId, userId)).thenReturn(Optional.empty());

        User user = User.builder().id(userId).firstName("Aarav").lastName("Patel").email("aarav@collobster.com").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(courseCertificateRepository.save(any(CourseCertificate.class))).thenAnswer(i -> {
            CourseCertificate cc = i.getArgument(0);
            cc.setId(UUID.randomUUID());
            return cc;
        });

        CourseCertificateDto dto = learningService.claimCertificate(courseId, userId);
        assertThat(dto).isNotNull();
        assertThat(dto.getCertificateNumber()).startsWith("COLLOB-");
        assertThat(dto.getVerificationHash()).isNotBlank();
        assertThat(dto.getRecipientName()).isEqualTo("Aarav Patel");
        verify(courseCertificateRepository).save(any(CourseCertificate.class));
    }

    @Test
    @DisplayName("Verify certificate returns certificate details or throws ResourceNotFoundException")
    void testVerifyCertificate() {
        String certNum = "COLLOB-2026-TEST1234";
        CourseCertificate cert = CourseCertificate.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .userId(userId)
                .recipientName("Aarav Patel")
                .certificateNumber(certNum)
                .verificationHash("abc123hash")
                .issuedAt(Instant.now())
                .build();

        when(courseCertificateRepository.findByCertificateNumber(certNum)).thenReturn(Optional.of(cert));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        CourseCertificateDto verified = learningService.verifyCertificate(certNum);
        assertThat(verified.getCertificateNumber()).isEqualTo(certNum);
        assertThat(verified.getRecipientName()).isEqualTo("Aarav Patel");

        when(courseCertificateRepository.findByCertificateNumber("NONEXISTENT")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> learningService.verifyCertificate("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
