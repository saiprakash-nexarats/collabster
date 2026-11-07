-- V37: Create Learning, Mentorship, and Careers Schema for Stage P2

-- ============================================================================
-- 1. LEARNING MODULE
-- ============================================================================

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    description TEXT,
    difficulty VARCHAR(32) NOT NULL DEFAULT 'beginner',
    category VARCHAR(64) NOT NULL DEFAULT 'Development',
    state VARCHAR(32) NOT NULL DEFAULT 'published',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_courses_difficulty CHECK (difficulty IN ('beginner', 'intermediate', 'advanced')),
    CONSTRAINT chk_courses_state CHECK (state IN ('draft', 'published', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_courses_instructor ON courses(instructor_id);
CREATE INDEX IF NOT EXISTS idx_courses_state ON courses(state);
CREATE INDEX IF NOT EXISTS idx_courses_category ON courses(category);

CREATE TABLE IF NOT EXISTS course_lessons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content TEXT,
    exercise_prompt TEXT,
    starter_code TEXT,
    order_index INT NOT NULL DEFAULT 0,
    duration_minutes INT NOT NULL DEFAULT 15,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_lessons_course ON course_lessons(course_id);
CREATE INDEX IF NOT EXISTS idx_course_lessons_order ON course_lessons(course_id, order_index);

CREATE TABLE IF NOT EXISTS course_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_state VARCHAR(32) NOT NULL DEFAULT 'active',
    progress_percent INT NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_course_enrollment UNIQUE (course_id, user_id),
    CONSTRAINT chk_course_enrollment_state CHECK (enrollment_state IN ('active', 'completed', 'dropped'))
);

CREATE INDEX IF NOT EXISTS idx_course_enrollments_user ON course_enrollments(user_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course ON course_enrollments(course_id);

CREATE TABLE IF NOT EXISTS lesson_completions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES course_lessons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lesson_completion UNIQUE (lesson_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_lesson_completions_user ON lesson_completions(user_id);
CREATE INDEX IF NOT EXISTS idx_lesson_completions_lesson ON lesson_completions(lesson_id);

CREATE TABLE IF NOT EXISTS course_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    certificate_number VARCHAR(64) NOT NULL UNIQUE,
    verification_hash VARCHAR(128) NOT NULL UNIQUE,
    recipient_name VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_certificate UNIQUE (course_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_course_certificates_user ON course_certificates(user_id);
CREATE INDEX IF NOT EXISTS idx_course_certificates_number ON course_certificates(certificate_number);

-- ============================================================================
-- 2. MENTORSHIP MODULE
-- ============================================================================

CREATE TABLE IF NOT EXISTS mentor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    headline VARCHAR(255) NOT NULL,
    bio TEXT,
    hourly_rate NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(8) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    skills TEXT,
    state VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mentor_profiles_state CHECK (state IN ('active', 'inactive'))
);

CREATE INDEX IF NOT EXISTS idx_mentor_profiles_user ON mentor_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_state ON mentor_profiles(state);

CREATE TABLE IF NOT EXISTS mentor_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    booked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_mentor_slots_mentor ON mentor_slots(mentor_id);
CREATE INDEX IF NOT EXISTS idx_mentor_slots_time ON mentor_slots(slot_time);

CREATE TABLE IF NOT EXISTS mentor_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    learner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_id UUID REFERENCES mentor_slots(id) ON DELETE SET NULL,
    session_type VARCHAR(64) NOT NULL DEFAULT '1:1 mentoring',
    note TEXT,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 30,
    state VARCHAR(32) NOT NULL DEFAULT 'requested',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mentor_sessions_state CHECK (state IN ('requested', 'confirmed', 'cancelled', 'completed'))
);

CREATE INDEX IF NOT EXISTS idx_mentor_sessions_mentor ON mentor_sessions(mentor_id);
CREATE INDEX IF NOT EXISTS idx_mentor_sessions_learner ON mentor_sessions(learner_id);
CREATE INDEX IF NOT EXISTS idx_mentor_sessions_state ON mentor_sessions(state);

-- ============================================================================
-- 3. CAREERS MODULE
-- ============================================================================

CREATE TABLE IF NOT EXISTS career_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(128) NOT NULL DEFAULT 'Flexible',
    work_mode VARCHAR(32) NOT NULL DEFAULT 'Flexible',
    employment_type VARCHAR(32) NOT NULL DEFAULT 'Full-time',
    salary_range VARCHAR(128),
    state VARCHAR(32) NOT NULL DEFAULT 'published',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_career_jobs_state CHECK (state IN ('published', 'closed', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_career_jobs_employer ON career_jobs(employer_id);
CREATE INDEX IF NOT EXISTS idx_career_jobs_state ON career_jobs(state);

CREATE TABLE IF NOT EXISTS career_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES career_jobs(id) ON DELETE CASCADE,
    applicant_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_object_id UUID REFERENCES storage_objects(id) ON DELETE SET NULL,
    note TEXT,
    state VARCHAR(32) NOT NULL DEFAULT 'submitted',
    review_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_job_applicant UNIQUE (job_id, applicant_id),
    CONSTRAINT chk_career_applications_state CHECK (state IN ('submitted', 'shortlisted', 'rejected', 'hired', 'withdrawn'))
);

CREATE INDEX IF NOT EXISTS idx_career_applications_job ON career_applications(job_id);
CREATE INDEX IF NOT EXISTS idx_career_applications_applicant ON career_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_career_applications_state ON career_applications(state);

