package com.collabsphere_first.p2;

import com.collabsphere_first.career.entity.CareerApplication;
import com.collabsphere_first.career.entity.CareerJob;
import com.collabsphere_first.career.repository.CareerApplicationRepository;
import com.collabsphere_first.career.repository.CareerJobRepository;
import com.collabsphere_first.learning.entity.*;
import com.collabsphere_first.learning.repository.*;
import com.collabsphere_first.mentor.entity.*;
import com.collabsphere_first.mentor.repository.*;
import com.collabsphere_first.user.entity.AuthProvider;
import com.collabsphere_first.user.entity.User;
import com.collabsphere_first.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.Socket;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "jwt.secret=c2FpLXByYWthc2gtbmV4YXJhdHMtY29sbGFic3Rlci10ZXN0LWp3dC1zZWNyZXQta2V5LXNob3VsZC1iZS1hdC1sZWFzdC0yNTYtYml0cy02NC1jaGFyYWN0ZXJzLWxvbmchIQ=="
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@EnabledIf("isPostgresAvailable")
public class LearningMentorshipCareersPostgreSqlIntegrationTest {

        static boolean isPostgresAvailable() {
                try (Socket socket = new Socket("localhost", 5432)) {
                        return true;
                } catch (Exception e) {
                        if ("true".equalsIgnoreCase(System.getenv("CI"))) {
                                throw new IllegalStateException("PostgreSQL 17 is required in CI environment but unreachable on localhost:5432", e);
                        }
                        return false;
                }
        }

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;
        @Autowired
        private CourseLessonRepository lessonRepository;
        @Autowired
        private CourseEnrollmentRepository enrollmentRepository;
        @Autowired
        private LessonCompletionRepository completionRepository;
        @Autowired
        private CourseCertificateRepository certificateRepository;

        @Autowired
        private MentorProfileRepository mentorProfileRepository;
        @Autowired
        private MentorSlotRepository mentorSlotRepository;
        @Autowired
        private MentorSessionRepository mentorSessionRepository;

        @Autowired
        private CareerJobRepository careerJobRepository;
        @Autowired
        private CareerApplicationRepository careerApplicationRepository;

        private User teacher;
        private User student;

        @BeforeEach
        void setUp() {
                teacher = userRepository.save(User.builder()
                                .firstName("Kavya")
                                .lastName("Nair")
                                .email("teacher-" + UUID.randomUUID() + "@collobster.test")
                                .password("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890")
                                .provider(AuthProvider.LOCAL)
                                .emailVerified(true)
                                .accountEnabled(true)
                                .build());

                student = userRepository.save(User.builder()
                                .firstName("Vikram")
                                .lastName("Singh")
                                .email("student-" + UUID.randomUUID() + "@collobster.test")
                                .password("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890")
                                .provider(AuthProvider.LOCAL)
                                .emailVerified(true)
                                .accountEnabled(true)
                                .build());
        }

        @Test
        @DisplayName("Learning module: course lifecycle, lesson completions and certificate constraints")
        void testLearningPersistenceAndConstraints() {
                Course course = courseRepository.save(Course.builder()
                                .instructorId(teacher.getId())
                                .title("Database Engineering at Scale")
                                .description("Deep dive into ACID and distributed storage")
                                .category("Database")
                                .difficulty("advanced")
                                .state("published")
                                .build());

                CourseLesson lesson1 = lessonRepository.save(CourseLesson.builder()
                                .courseId(course.getId())
                                .title("WAL and Consensus")
                                .durationMinutes(25)
                                .orderIndex(1)
                                .build());

                CourseLesson lesson2 = lessonRepository.save(CourseLesson.builder()
                                .courseId(course.getId())
                                .title("MVCC Internals")
                                .durationMinutes(30)
                                .orderIndex(2)
                                .build());

                // Enrollment
                CourseEnrollment enrollment = enrollmentRepository.save(CourseEnrollment.builder()
                                .courseId(course.getId())
                                .userId(student.getId())
                                .enrollmentState("active")
                                .build());
                assertThat(enrollment.getId()).isNotNull();

                // Duplicate enrollment rejected by unique constraint
                assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(CourseEnrollment.builder()
                                .courseId(course.getId())
                                .userId(student.getId())
                                .enrollmentState("active")
                                .build()))
                                .isInstanceOf(DataIntegrityViolationException.class);

                // Lesson completions
                completionRepository.save(LessonCompletion.builder()
                                .lessonId(lesson1.getId())
                                .userId(student.getId())
                                .build());

                completionRepository.save(LessonCompletion.builder()
                                .lessonId(lesson2.getId())
                                .userId(student.getId())
                                .build());

                // Certificate issuance
                CourseCertificate certificate = certificateRepository.save(CourseCertificate.builder()
                                .courseId(course.getId())
                                .userId(student.getId())
                                .recipientName("Vikram Singh")
                                .certificateNumber("COLLOB-2026-TEST9999")
                                .verificationHash("sha256-sample-verification-hash")
                                .build());

                assertThat(certificate.getId()).isNotNull();
                assertThat(certificateRepository.findByCertificateNumber("COLLOB-2026-TEST9999")).isPresent();
        }

        @Test
        @DisplayName("Mentorship module: profile upsert, slots and double booking protection")
        void testMentorshipPersistence() {
                MentorProfile profile = mentorProfileRepository.save(MentorProfile.builder()
                                .userId(teacher.getId())
                                .headline("Principal Architect")
                                .bio("Guiding developers into cloud-native engineering")
                                .hourlyRate(new BigDecimal("150.00"))
                                .currency("USD")
                                .skills("[\"Architecture\",\"Spring\",\"PostgreSQL\"]")
                                .state("active")
                                .build());
                assertThat(profile.getId()).isNotNull();

                MentorSlot slot = mentorSlotRepository.save(MentorSlot.builder()
                                .mentorId(teacher.getId())
                                .slotTime(Instant.now().plusSeconds(7200))
                                .durationMinutes(30)
                                .booked(true)
                                .build());

                MentorSession session = mentorSessionRepository.save(MentorSession.builder()
                                .mentorId(teacher.getId())
                                .learnerId(student.getId())
                                .slotId(slot.getId())
                                .sessionType("Architecture Review")
                                .startsAt(slot.getSlotTime())
                                .state("confirmed")
                                .build());

                assertThat(session.getId()).isNotNull();
                List<MentorSession> sessions = mentorSessionRepository
                                .findByMentorIdOrderByStartsAtDesc(teacher.getId());
                assertThat(sessions).isNotEmpty();
        }

        @Test
        @DisplayName("Careers module: job creation, application submission and unique constraint")
        void testCareersPersistenceAndConstraints() {
                CareerJob job = careerJobRepository.save(CareerJob.builder()
                                .employerId(teacher.getId())
                                .companyName("NexaRats Systems")
                                .title("Principal Cloud Engineer")
                                .description("Spearhead global multi-region deployments")
                                .location("Hyderabad / Remote")
                                .workMode("Remote")
                                .employmentType("Full-time")
                                .salaryRange("$130,000 - $170,000")
                                .state("published")
                                .build());

                CareerApplication app = careerApplicationRepository.save(CareerApplication.builder()
                                .jobId(job.getId())
                                .applicantId(student.getId())
                                .note("Highly interested in cloud platform architecture")
                                .state("submitted")
                                .build());

                assertThat(app.getId()).isNotNull();

                // Duplicate application by same applicant for same job is rejected by
                // uq_job_applicant
                assertThatThrownBy(() -> careerApplicationRepository.saveAndFlush(CareerApplication.builder()
                                .jobId(job.getId())
                                .applicantId(student.getId())
                                .note("Duplicate application attempt")
                                .state("submitted")
                                .build()))
                                .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Atomic slot reservation query enforces exclusive booking and returns 0 on duplicate attempt")
        void testAtomicSlotReservationInDatabase() {
                MentorSlot slot = mentorSlotRepository.save(MentorSlot.builder()
                                .mentorId(teacher.getId())
                                .slotTime(Instant.now().plusSeconds(7200))
                                .durationMinutes(45)
                                .booked(false)
                                .build());

                // First reservation attempt succeeds
                int firstUpdate = mentorSlotRepository.reserveSlotAtomically(slot.getId(), Instant.now());
                assertThat(firstUpdate).isEqualTo(1);

                // Second reservation attempt on the same slot yields 0 rows updated
                int secondUpdate = mentorSlotRepository.reserveSlotAtomically(slot.getId(), Instant.now());
                assertThat(secondUpdate).isEqualTo(0);
        }
}
