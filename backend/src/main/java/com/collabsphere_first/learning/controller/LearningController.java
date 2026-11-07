package com.collabsphere_first.learning.controller;

import com.collabsphere_first.community.dto.ApiResponse;
import com.collabsphere_first.learning.dto.*;
import com.collabsphere_first.learning.entity.CourseEnrollment;
import com.collabsphere_first.learning.service.LearningService;
import com.collabsphere_first.security.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/v1/learning", "/api/v1/learning"})
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;
    private final CurrentUserService currentUserService;

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<CourseDto>>> listCourses() {
        Long currentUserId = null;
        try {
            currentUserId = currentUserService.getCurrentUser().getId();
        } catch (Exception ignored) {}
        List<CourseDto> courses = learningService.listCourses(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<CourseDto>> createCourse(
            @Valid @RequestBody CreateCourseRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CourseDto course = learningService.createCourse(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(course));
    }

    @GetMapping("/courses/{id}/lessons")
    public ResponseEntity<ApiResponse<List<CourseLessonDto>>> listLessons(
            @PathVariable UUID id
    ) {
        Long currentUserId = null;
        try {
            currentUserId = currentUserService.getCurrentUser().getId();
        } catch (Exception ignored) {}
        List<CourseLessonDto> lessons = learningService.listLessons(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(lessons));
    }

    @PostMapping("/courses/{id}/lessons")
    public ResponseEntity<ApiResponse<CourseLessonDto>> addLesson(
            @PathVariable UUID id,
            @Valid @RequestBody AddLessonRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CourseLessonDto lesson = learningService.addLesson(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(lesson));
    }

    @PostMapping("/courses/{id}/enrollments")
    public ResponseEntity<ApiResponse<CourseEnrollment>> enroll(
            @PathVariable UUID id
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CourseEnrollment enrollment = learningService.enroll(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(enrollment));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeLesson(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        learningService.completeLesson(courseId, lessonId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lesson marked completed", null));
    }

    @PostMapping("/courses/{courseId}/certificate")
    public ResponseEntity<ApiResponse<CourseCertificateDto>> claimCertificate(
            @PathVariable UUID courseId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        CourseCertificateDto cert = learningService.claimCertificate(courseId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(cert));
    }

    @GetMapping("/certificates/my")
    public ResponseEntity<ApiResponse<List<CourseCertificateDto>>> listMyCertificates() {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        List<CourseCertificateDto> certs = learningService.listMyCertificates(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(certs));
    }

    @GetMapping("/certificates/verify/{certificateNumber}")
    public ResponseEntity<ApiResponse<CourseCertificateDto>> verifyCertificate(
            @PathVariable String certificateNumber
    ) {
        CourseCertificateDto cert = learningService.verifyCertificate(certificateNumber);
        return ResponseEntity.ok(ApiResponse.success(cert));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/exercise")
    public ResponseEntity<ApiResponse<LessonExerciseDto>> getLessonExercise(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId
    ) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        LessonExerciseDto exercise = learningService.getLessonExercise(courseId, lessonId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(exercise));
    }
}
