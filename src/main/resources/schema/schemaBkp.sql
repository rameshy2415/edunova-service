
-- =============================================================
--  EduNova School Management System
--  PostgreSQL Database Schema
--  Version: 1.0.0
-- =============================================================
-- Run order:
--   1. Extensions
--   2. Enums
--   3. Core / lookup tables (no FK dependencies)
--   4. User & auth tables
--   5. Academic structure tables
--   6. Student tables
--   7. Teacher tables
--   8. Attendance tables
--   9. Exams & grades tables
--  10. Fees & finance tables
--  11. Timetable tables
--  12. Communication tables
--  13. Audit / system tables
--  14. Indexes
--  15. Triggers
-- =============================================================

-- =============================================================
--  1. EXTENSIONS
-- =============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- trigram full-text search on names


-- =============================================================
--  2. ENUMS
-- =============================================================

CREATE TYPE user_role          AS ENUM ('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT');
CREATE TYPE gender_type        AS ENUM ('Male', 'Female', 'Other');
CREATE TYPE blood_group        AS ENUM ('A+','A-','B+','B-','AB+','AB-','O+','O-');
CREATE TYPE student_status     AS ENUM ('Active', 'Inactive', 'Suspended', 'Transferred', 'Graduated');
CREATE TYPE teacher_status     AS ENUM ('Active', 'On Leave', 'Resigned', 'Retired', 'Inactive');
CREATE TYPE fee_status         AS ENUM ('Paid', 'Partial', 'Overdue', 'Waived', 'Concession');
CREATE TYPE attendance_status  AS ENUM ('P', 'A', 'L', 'H');   -- Present, Absent, Late, Holiday
CREATE TYPE exam_status        AS ENUM ('Draft', 'Scheduled', 'Upcoming', 'Completed', 'Cancelled');
CREATE TYPE payment_method     AS ENUM ('Cash', 'Online', 'UPI', 'NEFT', 'Cheque', 'DD');
CREATE TYPE payment_status     AS ENUM ('Paid', 'Pending', 'Failed', 'Refunded');
CREATE TYPE day_of_week        AS ENUM ('Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday');
CREATE TYPE notice_audience    AS ENUM ('All', 'Students', 'Teachers', 'Parents', 'Admin');
CREATE TYPE category_type      AS ENUM ('General', 'OBC', 'SC', 'ST', 'EWS', 'Other');
CREATE TYPE board_type         AS ENUM ('CBSE', 'ICSE', 'IB', 'State', 'Other');


-- =============================================================
--  3. CORE / LOOKUP TABLES
-- =============================================================

-- ── Schools (multi-tenant ready) ──────────────────────────────
CREATE TABLE schools (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    board           board_type  NOT NULL DEFAULT 'CBSE',
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    phone           VARCHAR(20),
    email           VARCHAR(150),
    website         VARCHAR(200),
    logo_url        TEXT,
    motto           VARCHAR(300),
    established_year SMALLINT,
    affiliation_no  VARCHAR(50),
    principal_name  VARCHAR(150),
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Academic years ────────────────────────────────────────────
CREATE TABLE academic_years (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    label           VARCHAR(20) NOT NULL,           -- e.g. '2025-26'
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    is_current      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_year_dates CHECK (end_date > start_date),
    UNIQUE (school_id, label)
);

-- ── Terms / semesters ─────────────────────────────────────────
CREATE TABLE terms (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,           -- 'Term 1', 'Q1', etc.
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    is_current      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_term_dates CHECK (end_date > start_date)
);

-- ── Subjects ──────────────────────────────────────────────────
CREATE TABLE subjects (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(20),
    description     TEXT,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, name)
);

-- ── Houses ────────────────────────────────────────────────────
CREATE TABLE houses (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    color           VARCHAR(30),
    UNIQUE (school_id, name)
);

-- ── Rooms / classrooms ────────────────────────────────────────
CREATE TABLE rooms (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,           -- 'R-101', 'Lab-2'
    type            VARCHAR(50),                    -- 'Classroom', 'Lab', 'Hall'
    capacity        SMALLINT,
    floor           VARCHAR(20),
    building        VARCHAR(50),
    UNIQUE (school_id, name)
);


-- =============================================================
--  4. USERS & AUTH
-- =============================================================

-- ── Users (authentication) ────────────────────────────────────
CREATE TABLE users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        REFERENCES schools(id) ON DELETE CASCADE,
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(150) NOT NULL,
    mobile          VARCHAR(20)   UNIQUE,
    password_hash   TEXT        NOT NULL,
    role            user_role   NOT NULL,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    password_reset_token  TEXT,
    password_reset_expires TIMESTAMPTZ,
    avatar_url      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, email)
);

-- ── Refresh tokens (JWT rotation) ─────────────────────────────
CREATE TABLE refresh_tokens (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      TEXT        NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
--  5. ACADEMIC STRUCTURE
-- =============================================================

-- ── Grades / standard levels ──────────────────────────────────
-- e.g. Grade 9, Grade 10, Grade 11
CREATE TABLE grades (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(20) NOT NULL,           -- '9', '10', '11'
    display_name    VARCHAR(50),                    -- 'Class 9'
    sort_order      SMALLINT    NOT NULL DEFAULT 0,
    UNIQUE (school_id, name)
);

-- ── Sections / divisions ──────────────────────────────────────
-- e.g. Class 9-A, Class 9-B
CREATE TABLE sections (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    grade_id        UUID        NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id) ON DELETE CASCADE,
    name            VARCHAR(5)  NOT NULL,           -- 'A', 'B', 'C'
    display_name    VARCHAR(20),                    -- '9-A'
    capacity        SMALLINT    NOT NULL DEFAULT 40,
    class_teacher_id UUID,                          -- FK to teachers.id (added below)
    room_id         UUID        REFERENCES rooms(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (grade_id, academic_year_id, name)
);


-- =============================================================
--  6. STUDENTS
-- =============================================================

-- ── Student master record ─────────────────────────────────────
CREATE TABLE students (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    admission_no    VARCHAR(30) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    date_of_birth   DATE        NOT NULL,
    gender          gender_type NOT NULL,
    blood_group     blood_group,
    nationality     VARCHAR(60) NOT NULL DEFAULT 'Indian',
    religion        VARCHAR(60),
    category        category_type NOT NULL DEFAULT 'General',
    mother_tongue   VARCHAR(60),
    aadhaar_no      VARCHAR(12),                    -- encrypted at app level
    photo_url       TEXT,
    status          student_status NOT NULL DEFAULT 'Active',
    house_id        UUID        REFERENCES houses(id),
    previous_school VARCHAR(200),
    admission_date  DATE        NOT NULL DEFAULT CURRENT_DATE,
    leaving_date    DATE,
    leaving_reason  TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, admission_no)
);

-- ── Student section enrollment ────────────────────────────────
-- One row per student per academic year (tracks promotions)
CREATE TABLE student_enrollments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id      UUID        NOT NULL REFERENCES sections(id) ON DELETE RESTRICT,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    roll_number     SMALLINT    NOT NULL,
    is_current      BOOLEAN     NOT NULL DEFAULT TRUE,
    enrolled_at     DATE        NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (section_id, academic_year_id, roll_number),
    UNIQUE (student_id, academic_year_id)
);

-- ── Student contacts (parents / guardians) ────────────────────
CREATE TABLE student_contacts (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    user_id         UUID        REFERENCES users(id) ON DELETE SET NULL,  -- parent portal login
    relation        VARCHAR(30) NOT NULL,                                  -- 'Father', 'Mother', 'Guardian'
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(20) NOT NULL,
    alt_phone       VARCHAR(20),
    email           VARCHAR(150),
    occupation      VARCHAR(100),
    is_primary      BOOLEAN     NOT NULL DEFAULT FALSE,
    is_emergency    BOOLEAN     NOT NULL DEFAULT FALSE,
    address         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Student address ───────────────────────────────────────────
CREATE TABLE student_addresses (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    type            VARCHAR(20) NOT NULL DEFAULT 'Home',    -- 'Home', 'Permanent'
    line1           TEXT        NOT NULL,
    line2           TEXT,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    pincode         VARCHAR(10) NOT NULL,
    country         VARCHAR(60) NOT NULL DEFAULT 'India',
    is_current      BOOLEAN     NOT NULL DEFAULT TRUE
);

-- ── Student health info ───────────────────────────────────────
CREATE TABLE student_health (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL UNIQUE REFERENCES students(id) ON DELETE CASCADE,
    height_cm       NUMERIC(5,1),
    weight_kg       NUMERIC(5,1),
    allergies       TEXT,
    medical_conditions TEXT,
    medications     TEXT,
    doctor_name     VARCHAR(150),
    doctor_phone    VARCHAR(20),
    notes           TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Student vaccinations ──────────────────────────────────────
CREATE TABLE student_vaccinations (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    vaccine_name    VARCHAR(100) NOT NULL,
    dose            VARCHAR(20),
    date_given      DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'Completed',
    notes           TEXT
);

-- ── Student documents ─────────────────────────────────────────
CREATE TABLE student_documents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    type            VARCHAR(60) NOT NULL,     -- 'Birth Certificate', 'Transfer Certificate'
    file_url        TEXT        NOT NULL,
    file_name       VARCHAR(200),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    uploaded_by     UUID        REFERENCES users(id)
);


-- =============================================================
--  7. TEACHERS
-- =============================================================

-- ── Teacher master record ─────────────────────────────────────
CREATE TABLE teachers (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    employee_id     VARCHAR(30) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    date_of_birth   DATE,
    gender          gender_type,
    blood_group     blood_group,
    nationality     VARCHAR(60) NOT NULL DEFAULT 'Indian',
    religion        VARCHAR(60),
    category        category_type NOT NULL DEFAULT 'General',
    aadhaar_no      VARCHAR(12),
    pan_no          VARCHAR(10),
    phone           VARCHAR(20) NOT NULL,
    alt_phone       VARCHAR(20),
    email           VARCHAR(150) NOT NULL,
    address         TEXT,
    photo_url       TEXT,
    qualification   VARCHAR(200) NOT NULL,
    experience_years SMALLINT   NOT NULL DEFAULT 0,
    specialization  VARCHAR(200),
    joining_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    leaving_date    DATE,
    leaving_reason  TEXT,
    salary          NUMERIC(12,2),
    status          teacher_status NOT NULL DEFAULT 'Active',
    bio             TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, employee_id),
    UNIQUE (school_id, email)
);

-- ── Teacher subject assignments ───────────────────────────────
CREATE TABLE teacher_subjects (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id      UUID        NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id      UUID        NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    is_primary      BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (teacher_id, subject_id)
);

-- ── Teacher section assignments ───────────────────────────────
-- Which sections a teacher teaches in a given year
CREATE TABLE teacher_section_assignments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id      UUID        NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    section_id      UUID        NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    subject_id      UUID        NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    is_class_teacher BOOLEAN    NOT NULL DEFAULT FALSE,
    assigned_at     DATE        NOT NULL DEFAULT CURRENT_DATE,
    UNIQUE (teacher_id, section_id, subject_id, academic_year_id)
);

-- Back-fill FK for class teacher on sections
ALTER TABLE sections
    ADD CONSTRAINT fk_sections_class_teacher
    FOREIGN KEY (class_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL;


-- =============================================================
--  8. ATTENDANCE
-- =============================================================

-- ── Daily attendance ──────────────────────────────────────────
CREATE TABLE attendance (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id      UUID        NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    date            DATE        NOT NULL,
    status          attendance_status NOT NULL,
    period          SMALLINT,              -- NULL = whole-day; 1-8 for period-wise
    subject_id      UUID        REFERENCES subjects(id),
    marked_by       UUID        REFERENCES users(id),
    note            TEXT,
    marked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX uniq_attendance_student_date_period
ON attendance (student_id, date, COALESCE(period, 0));

-- ── Attendance summary (materialised monthly rollup) ──────────
CREATE TABLE attendance_summary (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id      UUID        NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    month           SMALLINT    NOT NULL,   -- 1-12
    year            SMALLINT    NOT NULL,
    working_days    SMALLINT    NOT NULL DEFAULT 0,
    present_days    SMALLINT    NOT NULL DEFAULT 0,
    absent_days     SMALLINT    NOT NULL DEFAULT 0,
    late_days       SMALLINT    NOT NULL DEFAULT 0,
    holiday_days    SMALLINT    NOT NULL DEFAULT 0,
    percentage      NUMERIC(5,2) GENERATED ALWAYS AS (
                        CASE WHEN working_days > 0
                             THEN ROUND((present_days::NUMERIC / working_days) * 100, 2)
                             ELSE 0 END
                    ) STORED,
    last_updated    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, month, year)
);

-- ── Holiday calendar ──────────────────────────────────────────
CREATE TABLE holidays (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    date            DATE        NOT NULL,
    name            VARCHAR(100) NOT NULL,
    type            VARCHAR(50),    -- 'National', 'Regional', 'School'
    UNIQUE (school_id, date)
);


-- =============================================================
--  9. EXAMS & GRADES
-- =============================================================

-- ── Exam definitions ──────────────────────────────────────────
CREATE TABLE exams (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    term_id         UUID        REFERENCES terms(id),
    name            VARCHAR(100) NOT NULL,          -- 'Unit Test 2', 'Mid-Term'
    type            VARCHAR(50) NOT NULL,           -- 'Unit Test', 'Mid-Term', 'Final', 'Practical'
    start_date      DATE,
    end_date        DATE,
    status          exam_status NOT NULL DEFAULT 'Draft',
    is_published    BOOLEAN     NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_by      UUID        REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Exam-subject-section schedule ─────────────────────────────
CREATE TABLE exam_schedules (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID        NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    subject_id      UUID        NOT NULL REFERENCES subjects(id),
    section_id      UUID        NOT NULL REFERENCES sections(id),
    exam_date       DATE        NOT NULL,
    start_time      TIME,
    end_time        TIME,
    room_id         UUID        REFERENCES rooms(id),
    max_marks       NUMERIC(6,2) NOT NULL DEFAULT 100,
    passing_marks   NUMERIC(6,2) NOT NULL DEFAULT 35,
    invigilator_id  UUID        REFERENCES teachers(id),
    UNIQUE (exam_id, subject_id, section_id)
);

-- ── Student results ───────────────────────────────────────────
CREATE TABLE exam_results (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_schedule_id UUID       NOT NULL REFERENCES exam_schedules(id) ON DELETE CASCADE,
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    marks_obtained  NUMERIC(6,2),
    grade           VARCHAR(5),                -- 'A+', 'A', 'B+', …, 'F'
    grade_points    NUMERIC(4,2),              -- GPA points
    remarks         TEXT,
    is_absent       BOOLEAN     NOT NULL DEFAULT FALSE,
    entered_by      UUID        REFERENCES users(id),
    entered_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (exam_schedule_id, student_id)
);

-- ── Grade scale configuration ─────────────────────────────────
CREATE TABLE grade_scales (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,           -- 'CBSE Standard'
    is_default      BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE grade_scale_entries (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    grade_scale_id  UUID        NOT NULL REFERENCES grade_scales(id) ON DELETE CASCADE,
    grade           VARCHAR(5)  NOT NULL,           -- 'A+'
    min_percentage  NUMERIC(5,2) NOT NULL,
    max_percentage  NUMERIC(5,2) NOT NULL,
    grade_points    NUMERIC(4,2),
    description     VARCHAR(50),                    -- 'Outstanding'
    CONSTRAINT chk_pct_range CHECK (max_percentage >= min_percentage)
);

-- ── Report cards ──────────────────────────────────────────────
CREATE TABLE report_cards (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    term_id         UUID        REFERENCES terms(id),
    total_marks     NUMERIC(8,2),
    max_total_marks NUMERIC(8,2),
    percentage      NUMERIC(5,2),
    overall_grade   VARCHAR(5),
    rank_in_class   SMALLINT,
    class_teacher_remarks TEXT,
    principal_remarks     TEXT,
    is_published    BOOLEAN     NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    generated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, academic_year_id, term_id)
);


-- =============================================================
--  10. FEES & FINANCE
-- =============================================================

-- ── Fee categories ────────────────────────────────────────────
CREATE TABLE fee_categories (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,          -- 'Tuition Fee', 'Transport Fee'
    description     TEXT,
    is_recurring    BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order      SMALLINT    NOT NULL DEFAULT 0,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE (school_id, name)
);

-- ── Fee structure (amount per grade per year) ─────────────────
CREATE TABLE fee_structures (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    fee_category_id UUID        NOT NULL REFERENCES fee_categories(id),
    grade_id        UUID        REFERENCES grades(id),      -- NULL = applies to all grades
    amount          NUMERIC(12,2) NOT NULL,
    frequency       VARCHAR(20) NOT NULL DEFAULT 'Annual',  -- 'Annual','Quarterly','Monthly','OneTime'
    due_day         SMALLINT,                               -- day of month due
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (academic_year_id, fee_category_id, grade_id)
);

-- ── Student fee assignment ────────────────────────────────────
-- What each student owes for the year (derived from fee_structures)
CREATE TABLE student_fee_assignments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    fee_category_id UUID        NOT NULL REFERENCES fee_categories(id),
    total_amount    NUMERIC(12,2) NOT NULL,
    concession_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    concession_reason TEXT,
    net_amount      NUMERIC(12,2) GENERATED ALWAYS AS (total_amount - concession_amount) STORED,
    due_date        DATE,
    status          fee_status  NOT NULL DEFAULT 'Overdue',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, academic_year_id, fee_category_id)
);

-- ── Fee payments (receipts) ───────────────────────────────────
CREATE TABLE fee_payments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_no      VARCHAR(40) NOT NULL UNIQUE,
    student_id      UUID        NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    fee_category_id UUID        NOT NULL REFERENCES fee_categories(id),
    amount_paid     NUMERIC(12,2) NOT NULL,
    payment_method  payment_method NOT NULL,
    payment_status  payment_status NOT NULL DEFAULT 'Paid',
    transaction_ref VARCHAR(100),                  -- UPI ref, cheque no, etc.
    payment_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    collected_by    UUID        REFERENCES users(id),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Expense categories ────────────────────────────────────────
CREATE TABLE expense_categories (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    UNIQUE (school_id, name)
);

-- ── Budget allocation ─────────────────────────────────────────
CREATE TABLE budgets (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    expense_category_id UUID   NOT NULL REFERENCES expense_categories(id),
    allocated_amount NUMERIC(14,2) NOT NULL,
    notes           TEXT,
    UNIQUE (academic_year_id, expense_category_id)
);

-- ── Expenses (outgoing) ───────────────────────────────────────
CREATE TABLE expenses (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    expense_category_id UUID   NOT NULL REFERENCES expense_categories(id),
    description     VARCHAR(300) NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    payment_method  payment_method NOT NULL,
    expense_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    vendor          VARCHAR(150),
    bill_ref        VARCHAR(100),
    approved_by     UUID        REFERENCES users(id),
    entered_by      UUID        REFERENCES users(id),
    receipt_url     TEXT,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
--  11. TIMETABLE
-- =============================================================

-- ── Timetable master ──────────────────────────────────────────
CREATE TABLE timetables (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_year_id UUID       NOT NULL REFERENCES academic_years(id),
    name            VARCHAR(100) NOT NULL,          -- 'Main Timetable 2025-26'
    effective_from  DATE        NOT NULL,
    effective_to    DATE,
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by      UUID        REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Period definitions ────────────────────────────────────────
CREATE TABLE periods (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    period_number   SMALLINT    NOT NULL,           -- 1, 2, 3, …
    name            VARCHAR(30),                    -- 'Period 1', 'Break'
    start_time      TIME        NOT NULL,
    end_time        TIME        NOT NULL,
    is_break        BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE (school_id, period_number),
    CONSTRAINT chk_period_times CHECK (end_time > start_time)
);

-- ── Timetable slots ───────────────────────────────────────────
CREATE TABLE timetable_slots (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    timetable_id    UUID        NOT NULL REFERENCES timetables(id) ON DELETE CASCADE,
    section_id      UUID        NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    period_id       UUID        NOT NULL REFERENCES periods(id) ON DELETE CASCADE,
    day             day_of_week NOT NULL,
    subject_id      UUID        REFERENCES subjects(id),
    teacher_id      UUID        REFERENCES teachers(id),
    room_id         UUID        REFERENCES rooms(id),
    UNIQUE (timetable_id, section_id, period_id, day)
);


-- =============================================================
--  12. COMMUNICATION
-- =============================================================

-- ── Notices / announcements ───────────────────────────────────
CREATE TABLE notices (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    body            TEXT        NOT NULL,
    audience        notice_audience NOT NULL DEFAULT 'All',
    publish_date    DATE        NOT NULL DEFAULT CURRENT_DATE,
    expiry_date     DATE,
    is_pinned       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by      UUID        REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Notice attachments ────────────────────────────────────────
CREATE TABLE notice_attachments (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    notice_id       UUID        NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    file_url        TEXT        NOT NULL,
    file_name       VARCHAR(200),
    file_type       VARCHAR(50)
);

-- ── Messages (parent ↔ teacher / admin) ──────────────────────
CREATE TABLE messages (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    sender_id       UUID        NOT NULL REFERENCES users(id),
    recipient_id    UUID        NOT NULL REFERENCES users(id),
    subject         VARCHAR(200),
    body            TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    parent_message_id UUID      REFERENCES messages(id),     -- for threading
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Notifications (in-app) ────────────────────────────────────
CREATE TABLE notifications (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(60) NOT NULL,       -- 'fee_reminder', 'low_attendance', 'result_published'
    title           VARCHAR(200) NOT NULL,
    body            TEXT,
    reference_type  VARCHAR(60),               -- 'student', 'exam', 'payment'
    reference_id    UUID,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- =============================================================
--  13. AUDIT / SYSTEM
-- =============================================================

-- ── Audit log ─────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        REFERENCES schools(id),
    user_id         UUID        REFERENCES users(id),
    action          VARCHAR(60) NOT NULL,        -- 'CREATE', 'UPDATE', 'DELETE', 'LOGIN'
    entity_type     VARCHAR(60),                 -- 'student', 'teacher', 'payment'
    entity_id       UUID,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── App settings (per school) ─────────────────────────────────
CREATE TABLE school_settings (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    key             VARCHAR(100) NOT NULL,
    value           TEXT,
    updated_by      UUID        REFERENCES users(id),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, key)
);


-- =============================================================
--  13-A. SUPER ADMIN — SUBSCRIPTIONS & ONBOARDING
-- =============================================================

-- ── Subscription plans (master) ───────────────────────────────
CREATE TABLE subscription_plans (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL UNIQUE,        -- 'Basic', 'Pro', 'Enterprise'
    max_students    INT,                                 -- NULL = unlimited
    max_teachers    INT,
    price_monthly   NUMERIC(10,2),                      -- NULL = custom/enterprise
    price_annually  NUMERIC(10,2),
    features        JSONB       NOT NULL DEFAULT '[]',  -- ["attendance", "fees", ...]
    is_active       BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order      SMALLINT    NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── School subscriptions (per school) ─────────────────────────
-- One active row per school at any time.
-- Status lifecycle: trial → active → expired | suspended | cancelled
CREATE TYPE subscription_status AS ENUM (
    'trial', 'active', 'expired', 'suspended', 'cancelled'
);
CREATE TYPE billing_cycle AS ENUM ('monthly', 'quarterly', 'annual', 'custom');

CREATE TABLE subscriptions (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    plan_id             UUID        NOT NULL REFERENCES subscription_plans(id),
    status              subscription_status NOT NULL DEFAULT 'trial',
    billing_cycle       billing_cycle       NOT NULL DEFAULT 'annual',
    amount_override     NUMERIC(12,2),          -- NULL = use plan price
    discount_pct        NUMERIC(5,2)  NOT NULL DEFAULT 0,
    trial_ends_at       TIMESTAMPTZ,
    current_period_start DATE        NOT NULL DEFAULT CURRENT_DATE,
    current_period_end   DATE        NOT NULL,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason TEXT,
    internal_notes      TEXT,
    created_by          UUID        REFERENCES users(id),   -- superadmin who created
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (school_id)                                       -- one active sub per school
);

-- History of all subscription changes
CREATE TABLE subscription_history (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID        NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    school_id       UUID        NOT NULL REFERENCES schools(id),
    changed_by      UUID        REFERENCES users(id),
    change_type     VARCHAR(30) NOT NULL,    -- 'created','upgraded','downgraded','renewed','suspended','cancelled'
    old_plan        VARCHAR(50),
    new_plan        VARCHAR(50),
    old_status      subscription_status,
    new_status      subscription_status,
    notes           TEXT,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── School onboarding log ──────────────────────────────────────
-- Full audit trail of every school creation + admin account setup.
CREATE TABLE school_onboarding_log (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id           UUID        NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    onboarded_by        UUID        NOT NULL REFERENCES users(id),  -- superadmin
    plan_id             UUID        REFERENCES subscription_plans(id),
    admin_user_id       UUID        REFERENCES users(id),           -- created admin account
    admin_email         VARCHAR(150) NOT NULL,
    admin_name          VARCHAR(150) NOT NULL,
    welcome_email_sent  BOOLEAN     NOT NULL DEFAULT FALSE,
    welcome_email_sent_at TIMESTAMPTZ,
    notes               TEXT,
    onboarded_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Super admin users ──────────────────────────────────────────
-- Superadmins are NOT scoped to a school (school_id = NULL in users).
-- This table holds extra SA-specific fields.
CREATE TABLE super_admins (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name       VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    permissions     JSONB       NOT NULL DEFAULT '["all"]',  -- fine-grained perms if needed
    is_owner        BOOLEAN     NOT NULL DEFAULT FALSE,      -- founder/root admin
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Platform-level audit log (superadmin actions) ─────────────
CREATE TABLE superadmin_audit_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    superadmin_id   UUID        NOT NULL REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,      -- 'onboard_school', 'change_plan', 'reset_admin_password', ...
    target_type     VARCHAR(60),               -- 'school', 'subscription', 'admin_user'
    target_id       UUID,
    target_name     VARCHAR(200),              -- human-readable label for logs
    old_values      JSONB,
    new_values      JSONB,
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);



-- Subscriptions
CREATE INDEX idx_subscriptions_school      ON subscriptions (school_id);
CREATE INDEX idx_subscriptions_status      ON subscriptions (status);
CREATE INDEX idx_subscriptions_period_end  ON subscriptions (current_period_end);
CREATE INDEX idx_sub_history_school        ON subscription_history (school_id);
CREATE INDEX idx_sub_history_changed_at    ON subscription_history (changed_at DESC);
CREATE INDEX idx_onboarding_log_school     ON school_onboarding_log (school_id);
CREATE INDEX idx_sa_audit_action           ON superadmin_audit_log (action);
CREATE INDEX idx_sa_audit_created          ON superadmin_audit_log (created_at DESC);
CREATE INDEX idx_sa_audit_target           ON superadmin_audit_log (target_type, target_id);


CREATE INDEX idx_users_email             ON users (email);

-- Students
CREATE INDEX idx_students_school         ON students (school_id);
CREATE INDEX idx_students_status         ON students (status);
CREATE INDEX idx_students_name_trgm      ON students USING gin (name gin_trgm_ops);
CREATE INDEX idx_enrollments_student     ON student_enrollments (student_id);
CREATE INDEX idx_enrollments_section     ON student_enrollments (section_id);
CREATE INDEX idx_enrollments_year        ON student_enrollments (academic_year_id);

-- Teachers
CREATE INDEX idx_teachers_school         ON teachers (school_id);
CREATE INDEX idx_teachers_status         ON teachers (status);
CREATE INDEX idx_teachers_name_trgm      ON teachers USING gin (name gin_trgm_ops);
CREATE INDEX idx_tsa_teacher             ON teacher_section_assignments (teacher_id);
CREATE INDEX idx_tsa_section             ON teacher_section_assignments (section_id);

-- Attendance
CREATE INDEX idx_attendance_student_date ON attendance (student_id, date);
CREATE INDEX idx_attendance_section_date ON attendance (section_id, date);
CREATE INDEX idx_attendance_date         ON attendance (date);
CREATE INDEX idx_att_summary_student     ON attendance_summary (student_id);

-- Exams & results
CREATE INDEX idx_exams_school_year       ON exams (school_id, academic_year_id);
CREATE INDEX idx_exam_results_student    ON exam_results (student_id);
CREATE INDEX idx_exam_results_schedule   ON exam_results (exam_schedule_id);

-- Fees
CREATE INDEX idx_fee_payments_student    ON fee_payments (student_id);
CREATE INDEX idx_fee_payments_date       ON fee_payments (payment_date);
CREATE INDEX idx_fee_assignments_student ON student_fee_assignments (student_id);
CREATE INDEX idx_fee_assignments_status  ON student_fee_assignments (status);

-- Timetable
CREATE INDEX idx_timetable_slots_section ON timetable_slots (section_id, day);
CREATE INDEX idx_timetable_slots_teacher ON timetable_slots (teacher_id, day);

-- Audit
CREATE INDEX idx_audit_entity            ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_user              ON audit_logs (user_id);
CREATE INDEX idx_audit_created           ON audit_logs (created_at DESC);

-- Notifications
CREATE INDEX idx_notifications_user      ON notifications (user_id, is_read);


-- =============================================================
--  15. TRIGGERS
-- =============================================================

-- ── updated_at auto-update function ──────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

-- Apply to all tables that have updated_at
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'schools', 'users', 'students', 'teachers',
        'student_fee_assignments', 'exams', 'report_cards', 'notices'
    ] LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%s_updated_at
             BEFORE UPDATE ON %s
             FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            t, t
        );
    END LOOP;
END;
$$;

-- ── Receipt number auto-generation ───────────────────────────
CREATE SEQUENCE IF NOT EXISTS receipt_seq START 1000 INCREMENT 1;

CREATE OR REPLACE FUNCTION generate_receipt_no()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.receipt_no IS NULL OR NEW.receipt_no = '' THEN
        NEW.receipt_no := 'RCP-' || TO_CHAR(NOW(), 'YYYY') || '-' ||
                          LPAD(nextval('receipt_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_fee_payments_receipt_no
    BEFORE INSERT ON fee_payments
    FOR EACH ROW EXECUTE FUNCTION generate_receipt_no();

-- ── Attendance summary refresh ────────────────────────────────
CREATE OR REPLACE FUNCTION refresh_attendance_summary()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_month  SMALLINT := EXTRACT(MONTH FROM NEW.date);
    v_year   SMALLINT := EXTRACT(YEAR  FROM NEW.date);
BEGIN
    INSERT INTO attendance_summary (
        student_id, section_id, academic_year_id,
        month, year, working_days, present_days, absent_days, late_days, holiday_days
    )
    SELECT
        NEW.student_id,
        NEW.section_id,
        NEW.academic_year_id,
        v_month, v_year,
        COUNT(*) FILTER (WHERE status <> 'H'),
        COUNT(*) FILTER (WHERE status = 'P'),
        COUNT(*) FILTER (WHERE status = 'A'),
        COUNT(*) FILTER (WHERE status = 'L'),
        COUNT(*) FILTER (WHERE status = 'H')
    FROM attendance
    WHERE student_id      = NEW.student_id
      AND EXTRACT(MONTH FROM date) = v_month
      AND EXTRACT(YEAR  FROM date) = v_year
    ON CONFLICT (student_id, month, year)
    DO UPDATE SET
        working_days = EXCLUDED.working_days,
        present_days = EXCLUDED.present_days,
        absent_days  = EXCLUDED.absent_days,
        late_days    = EXCLUDED.late_days,
        holiday_days = EXCLUDED.holiday_days,
        last_updated = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_attendance_summary_refresh
    AFTER INSERT OR UPDATE ON attendance
    FOR EACH ROW EXECUTE FUNCTION refresh_attendance_summary();


-- =============================================================
--  SEED: default grade scale (CBSE)
-- =============================================================
-- Run after inserting your school record.
-- Replace '00000000-0000-0000-0000-000000000001' with your school UUID.

/*
INSERT INTO grade_scales (school_id, name, is_default)
VALUES ('YOUR_SCHOOL_UUID', 'CBSE Standard', TRUE)
RETURNING id;

INSERT INTO grade_scale_entries (grade_scale_id, grade, min_percentage, max_percentage, grade_points, description)
VALUES
    ('YOUR_SCALE_UUID', 'A+', 91, 100,  10.0, 'Outstanding'),
    ('YOUR_SCALE_UUID', 'A',  81,  90,   9.0, 'Excellent'),
    ('YOUR_SCALE_UUID', 'B+', 71,  80,   8.0, 'Very Good'),
    ('YOUR_SCALE_UUID', 'B',  61,  70,   7.0, 'Good'),
    ('YOUR_SCALE_UUID', 'C',  51,  60,   6.0, 'Average'),
    ('YOUR_SCALE_UUID', 'D',  33,  50,   5.0, 'Needs Improvement'),
    ('YOUR_SCALE_UUID', 'E',  21,  32,   4.0, 'Poor'),
    ('YOUR_SCALE_UUID', 'F',   0,  20,   0.0, 'Fail');
*/



INSERT INTO users (
  first_name,
  last_name,
    email,
  mobile,
    password_hash,
    role
) VALUES (
  'Ramesh',
  'Yadav',
    'superadmin@edunova.app',
  '8270078469',
    '{bcrypt}$2a$12$FcKHV1qRtuNwNNPi7eYfTerwN49Wkc/sLF60RB.L0FIBHz6haTQbG',
    'SUPER_ADMIN'::user_role
);