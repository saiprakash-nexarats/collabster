package com.collabsphere_first.learning.service;

import com.collabsphere_first.learning.dto.*;
import com.collabsphere_first.learning.entity.CourseEnrollment;

import java.util.List;
import java.util.UUID;

public interface LearningService {
    List<CourseDto> listCourses(Long userId);
    CourseDto createCourse(CreateCourseRequest request, Long userId);
    List<CourseLessonDto> listLessons(UUID courseId, Long userId);
    CourseLessonDto addLesson(UUID courseId, AddLessonRequest request, Long userId);
    CourseEnrollment enroll(UUID courseId, Long userId);
    void completeLesson(UUID courseId, UUID lessonId, Long userId);
    CourseCertificateDto claimCertificate(UUID courseId, Long userId);
    List<CourseCertificateDto> listMyCertificates(Long userId);
    CourseCertificateDto verifyCertificate(String certificateNumber);
    LessonExerciseDto getLessonExercise(UUID courseId, UUID lessonId, Long userId);
}

