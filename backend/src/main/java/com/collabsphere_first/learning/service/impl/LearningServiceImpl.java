package com.collabsphere_first.learning.service.impl;

import com.collabsphere_first.community.exception.ResourceNotFoundException;
import com.collabsphere_first.learning.dto.*;
import com.collabsphere_first.learning.entity.*;
import com.collabsphere_first.learning.repository.*;
import com.collabsphere_first.learning.service.LearningService;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningServiceImpl implements LearningService {

    private final CourseRepository courseRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final LessonCompletionRepository lessonCompletionRepository;
    private final CourseCertificateRepository courseCertificateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> listCourses(Long userId) {
        List<Course> courses = courseRepository.findByStateOrderByCreatedAtDesc("published");
        Map<UUID, CourseEnrollment> enrollments = (userId != null)
                ? courseEnrollmentRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(CourseEnrollment::getCourseId, e -> e))
                : Collections.emptyMap();

        return courses.stream().map(c -> {
            CourseEnrollment enrollment = enrollments.get(c.getId());
            return CourseDto.builder()
                    .id(c.getId())
                    .instructorId(c.getInstructorId())
                    .title(c.getTitle())
                    .summary(c.getSummary())
                    .description(c.getDescription())
                    .difficulty(c.getDifficulty())
                    .category(c.getCategory())
                    .state(c.getState())
                    .enrollmentState(enrollment != null ? enrollment.getEnrollmentState() : "unregistered")
                    .progress(enrollment != null ? enrollment.getProgressPercent() : 0)
                    .createdAt(c.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseDto createCourse(CreateCourseRequest request, Long userId) {
        Course course = Course.builder()
                .instructorId(userId)
                .title(request.getTitle().trim())
                .summary(request.getSummary())
                .description(request.getDescription())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "beginner")
                .category(request.getCategory() != null ? request.getCategory() : "Development")
                .state("published")
                .build();

        Course saved = courseRepository.save(course);
        return CourseDto.builder()
                .id(saved.getId())
                .instructorId(saved.getInstructorId())
                .title(saved.getTitle())
                .summary(saved.getSummary())
                .description(saved.getDescription())
                .difficulty(saved.getDifficulty())
                .category(saved.getCategory())
                .state(saved.getState())
                .enrollmentState("unregistered")
                .progress(0)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseLessonDto> listLessons(UUID courseId, Long userId) {
        List<CourseLesson> lessons = courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        List<UUID> lessonIds = lessons.stream().map(CourseLesson::getId).collect(Collectors.toList());

        Set<UUID> completedIds = (userId != null && !lessonIds.isEmpty())
                ? lessonCompletionRepository.findByUserIdAndLessonIdIn(userId, lessonIds).stream()
                .map(LessonCompletion::getLessonId)
                .collect(Collectors.toSet())
                : Collections.emptySet();

        return lessons.stream().map(l -> CourseLessonDto.builder()
                .id(l.getId())
                .courseId(l.getCourseId())
                .title(l.getTitle())
                .summary(l.getSummary())
                .content(l.getContent())
                .orderIndex(l.getOrderIndex())
                .durationMinutes(l.getDurationMinutes())
                .completed(completedIds.contains(l.getId()))
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseLessonDto addLesson(UUID courseId, AddLessonRequest request, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        if (!course.getInstructorId().equals(userId)) {
            throw new AccessDeniedException("Only the course instructor can add lessons");
        }

        int nextIndex = request.getOrderIndex() != null
                ? request.getOrderIndex()
                : (int) courseLessonRepository.countByCourseId(courseId);

        CourseLesson lesson = CourseLesson.builder()
                .courseId(courseId)
                .title(request.getTitle().trim())
                .summary(request.getSummary())
                .content(request.getContent())
                .exercisePrompt(request.getExercisePrompt())
                .starterCode(request.getStarterCode())
                .orderIndex(nextIndex)
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 15)
                .build();

        CourseLesson saved = courseLessonRepository.save(lesson);
        return CourseLessonDto.builder()
                .id(saved.getId())
                .courseId(saved.getCourseId())
                .title(saved.getTitle())
                .summary(saved.getSummary())
                .content(saved.getContent())
                .orderIndex(saved.getOrderIndex())
                .durationMinutes(saved.getDurationMinutes())
                .completed(false)
                .build();
    }

    @Override
    @Transactional
    public CourseEnrollment enroll(UUID courseId, Long userId) {
        courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        return courseEnrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> courseEnrollmentRepository.save(CourseEnrollment.builder()
                        .courseId(courseId)
                        .userId(userId)
                        .enrollmentState("active")
                        .progressPercent(0)
                        .build()));
    }

    @Override
    @Transactional
    public void completeLesson(UUID courseId, UUID lessonId, Long userId) {
        CourseLesson lesson = courseLessonRepository.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new NoSuchElementException("Lesson not found: " + lessonId));

        CourseEnrollment enrollment = courseEnrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> courseEnrollmentRepository.save(CourseEnrollment.builder()
                        .courseId(courseId)
                        .userId(userId)
                        .enrollmentState("active")
                        .progressPercent(0)
                        .build()));

        if (!lessonCompletionRepository.existsByLessonIdAndUserId(lessonId, userId)) {
            lessonCompletionRepository.save(LessonCompletion.builder()
                    .lessonId(lessonId)
                    .userId(userId)
                    .build());
        }

        List<CourseLesson> allLessons = courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        if (!allLessons.isEmpty()) {
            List<UUID> lessonIds = allLessons.stream().map(CourseLesson::getId).collect(Collectors.toList());
            long completedCount = lessonCompletionRepository.countByUserIdAndLessonIdIn(userId, lessonIds);
            int percent = (int) Math.round(((double) completedCount / allLessons.size()) * 100);
            enrollment.setProgressPercent(percent);
            if (percent >= 100) {
                enrollment.setEnrollmentState("completed");
                enrollment.setCompletedAt(Instant.now());
            }
            courseEnrollmentRepository.save(enrollment);
        }
    }

    @Override
    @Transactional
    public CourseCertificateDto claimCertificate(UUID courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NoSuchElementException("Course not found: " + courseId));

        List<CourseLesson> lessons = courseLessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        if (lessons.isEmpty()) {
            throw new IllegalStateException("Course has no lessons to certify completion");
        }

        List<UUID> lessonIds = lessons.stream().map(CourseLesson::getId).collect(Collectors.toList());
        long completedCount = lessonCompletionRepository.countByUserIdAndLessonIdIn(userId, lessonIds);
        if (completedCount < lessons.size()) {
            throw new IllegalStateException("All lessons must be completed to claim a certificate. ("
                    + completedCount + "/" + lessons.size() + " completed)");
        }

        Optional<CourseCertificate> existing = courseCertificateRepository.findByCourseIdAndUserId(courseId, userId);
        if (existing.isPresent()) {
            CourseCertificate cert = existing.get();
            return CourseCertificateDto.builder()
                    .id(cert.getId())
                    .courseId(cert.getCourseId())
                    .courseTitle(course.getTitle())
                    .recipientName(cert.getRecipientName())
                    .certificateNumber(cert.getCertificateNumber())
                    .verificationHash(cert.getVerificationHash())
                    .issuedAt(cert.getIssuedAt())
                    .build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        String recipientName = (user.getFirstName() + " " + user.getLastName()).trim();
        if (recipientName.isBlank()) recipientName = "Collobster Scholar";

        String certNumber = "COLLOB-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String hash = computeSha256(certNumber + ":" + userId + ":" + courseId);

        CourseCertificate cert = CourseCertificate.builder()
                .courseId(courseId)
                .userId(userId)
                .recipientName(recipientName)
                .certificateNumber(certNumber)
                .verificationHash(hash)
                .build();

        CourseCertificate saved = courseCertificateRepository.save(cert);
        return CourseCertificateDto.builder()
                .id(saved.getId())
                .courseId(saved.getCourseId())
                .courseTitle(course.getTitle())
                .recipientName(saved.getRecipientName())
                .certificateNumber(saved.getCertificateNumber())
                .verificationHash(saved.getVerificationHash())
                .issuedAt(saved.getIssuedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseCertificateDto> listMyCertificates(Long userId) {
        List<CourseCertificate> certs = courseCertificateRepository.findByUserIdOrderByIssuedAtDesc(userId);
        Map<UUID, String> titles = courseRepository.findAllById(certs.stream().map(CourseCertificate::getCourseId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Course::getId, Course::getTitle));

        return certs.stream().map(c -> CourseCertificateDto.builder()
                .id(c.getId())
                .courseId(c.getCourseId())
                .courseTitle(titles.getOrDefault(c.getCourseId(), "Completed Course"))
                .recipientName(c.getRecipientName())
                .certificateNumber(c.getCertificateNumber())
                .verificationHash(c.getVerificationHash())
                .issuedAt(c.getIssuedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseCertificateDto verifyCertificate(String certificateNumber) {
        CourseCertificate cert = courseCertificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found for number: " + certificateNumber));

        Course course = courseRepository.findById(cert.getCourseId()).orElse(null);

        return CourseCertificateDto.builder()
                .id(cert.getId())
                .courseId(cert.getCourseId())
                .courseTitle(course != null ? course.getTitle() : "Verified Course")
                .recipientName(cert.getRecipientName())
                .certificateNumber(cert.getCertificateNumber())
                .verificationHash(cert.getVerificationHash())
                .issuedAt(cert.getIssuedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LessonExerciseDto getLessonExercise(UUID courseId, UUID lessonId, Long userId) {
        CourseLesson lesson = courseLessonRepository.findByCourseIdAndId(courseId, lessonId)
                .orElseThrow(() -> new NoSuchElementException("Lesson not found: " + lessonId));

        return LessonExerciseDto.builder()
                .lessonId(lesson.getId())
                .exercisePrompt(lesson.getExercisePrompt() != null ? lesson.getExercisePrompt() : "Review the lesson materials and complete the challenge.")
                .starterCode(lesson.getStarterCode() != null ? lesson.getStarterCode() : "// Starter code here")
                .build();
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
