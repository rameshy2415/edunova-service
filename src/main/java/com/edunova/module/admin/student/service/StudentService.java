package com.edunova.module.admin.student.service;


import com.edunova.config.TenantContext;
import com.edunova.exception.ErrorCode;
import com.edunova.exception.AppException;
import com.edunova.filter.LoggedInUserContextDetails;
import com.edunova.module.admin.student.dto.*;
import com.edunova.module.admin.student.entity.*;
import com.edunova.module.admin.student.mapper.StudentMapper;
import com.edunova.module.admin.student.repository.*;
import com.edunova.module.admin.student.util.AdmissionNumberGenerator;
import com.edunova.notification.email.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentParentMappingRepository mappingRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final GuardianRepository guardianRepository;
    private final SectionRepository sectionRepository;
   // private final AcademicYearService            academicYearService;
    private final ParentService                  parentService;
    private final AdmissionNumberGenerator admissionGenerator;
    private final StudentMapper mapper;
    private final AcademicYearRepository academicYearRepository;
    private final EmailNotificationService emailService;

    // ── Enroll new student ─────────────────────────────────────
    @Transactional
    public StudentDto.Response enroll(StudentEnrollmentRequest request) {
        UUID schoolId = request.getSchoolId();
        UUID createdBy = getCurrentUserId();

        // Auto-generate admission number if not provided
       /* String admissionNo = request.getAdmissionNo();
        if (admissionNo == null || admissionNo.isBlank()) {
            admissionNo = admissionGenerator.generate(schoolId);
        } else {
            // Validate uniqueness of provided admission number
            if (studentRepository.existsBySchoolIdAndAdmissionNo(
                    schoolId, admissionNo)) {
                throw new AppException(ErrorCode.ADMISSION_NO_EXISTS);
            }
        }*/

        String admissionNo = admissionGenerator.generate(schoolId);

        // Create student record
        Student student = Student.builder()
                .schoolId(schoolId)
                .admissionNo(admissionNo)
                .name(request.getName())
                //.firstName(request.getFirstName().trim())
                //.lastName(request.getLastName().trim())
                .dateOfBirth(request.getDob())
                .gender(request.getGender())
                .address(request.getAddress())
                .bloodGroup(request.getBlood())
                .religion(request.getReligion())
                .category(request.getCategory())
                .motherTongue(request.getMother())
                .aadhaarNo(request.getAadhaarNo())
                .status("Active")
                .address(request.getAddress())
                .previousSchool(request.getPreviousSchool())
                //.notes(request.getN)
                //.createdBy(createdBy)
                .build();

        student = studentRepository.save(student);
        //log.info("Student enrolled: {} {} [{}]", student.getFirstName(), student.getLastName(), student.getId());
        log.info("Student enrolled: {} [{}]", student.getName(), student.getId());

        // Create and link parents (min 1, max 2 enforced by DTO validation)
        StudentGuardian guardian = StudentGuardian.builder()
                .studentId(student.getId())
                .address(student.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .altPhone(request.getAltPhone())
                .emergencyContact(request.getEmergencyContact())
                .father(request.getFather())
                .mother(request.getMother())
                .build();
        guardianRepository.save(guardian);
        /*boolean primarySet = false;
        for (ParentDto.CreateRequest parentRequest : request.getParents()) {
            Parent parent = parentService.createParent(schoolId, parentRequest);

            // First parent is primary if none explicitly marked
            boolean isPrimary = Boolean.TRUE.equals(parentRequest.getIsPrimary())
                    || (!primarySet);
            if (isPrimary) primarySet = true;

            StudentParentMapping mapping = StudentParentMapping.builder()
                    .student(student)
                    .parent(parent)
                    .isPrimary(isPrimary)
                    .build();

            mappingRepository.save(mapping);
        }*/

        // Enroll into section if sectionId provided
       /* StudentEnrollment enrollment = null;
        if (request.getSectionId() != null) {
            enrollment = enrollIntoSection(student, schoolId, request.getSectionId(), request.getRollNumber(), null);
        }*/

        StudentEnrollment enrollment = null;
        if (request.getSection() != null) {
            enrollment = enrollIntoSection(student, schoolId, request.getSection(), request.getRoll(), request.getAcademicYearId());
        }

        var response = buildFullResponse(student, enrollment);

        // 5. Audit / onboarding log
        emailService.sendStudentWelcomeEmail(
                request.getEmail(),
                LoggedInUserContextDetails.getCurrentUser(),
                response
        );


        return response;
    }

    // ── Get student by ID ──────────────────────────────────────
    public StudentResponseDTO getById(UUID studentId) {
        //UUID schoolId = TenantContext.getTenantId();

       /* Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));*/

        StudentResponseDTO student = studentRepository
                .findByStudentId(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

       /* AcademicYear currentYear = academicYearService.getCurrentYearEntity(schoolId);

        StudentEnrollment enrollment = enrollmentRepository
                .findDetailedEnrollment(studentId, currentYear.getId())
                .orElse(null);*/

        return student;
        //return buildFullResponse(student, enrollment);
    }

    // ── Get student by SchoolID ──────────────────────────────────────
    public List<StudentResponseDTO> getBySchoolId(UUID schoolId) {

        //List<StudentResponseDTO> student = studentRepository.findBySchoolId(schoolId).orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

       /* AcademicYear currentYear = academicYearService
                .getCurrentYearEntity(schoolId);

        StudentEnrollment enrollment = enrollmentRepository.findDetailedEnrollment(studentId, currentYear.getId())
                .orElse(null);*/

        //return buildFullResponse(student, enrollment);

        return studentRepository.findBySchoolId(schoolId);
    }

    // ── Search / list students ─────────────────────────────────
    public Page<StudentDto.Response> search(String search,
                                             Boolean isActive,
                                             Pageable pageable) {
        UUID schoolId = TenantContext.getTenantId();
        boolean active = isActive != null ? isActive : true;

        return studentRepository
                .searchStudents(schoolId, isActive, search, pageable)
                .map(s -> buildFullResponse(s, null));
    }

    // ── Update student ─────────────────────────────────────────
    @Transactional
    public StudentDto.Response update(UUID studentId,
                                       StudentDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

       /* if (request.getFirstName()   != null)
            student.setFirstName(request.getFirstName().trim());
        if (request.getLastName()    != null)
            student.setLastName(request.getLastName().trim());*/
        if (request.getDateOfBirth() != null)
            student.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender()      != null)
            student.setGender(request.getGender());
        if (request.getAddress()     != null)
            student.setAddress(request.getAddress());
        if (request.getBloodGroup()  != null)
            student.setBloodGroup(request.getBloodGroup());
        if (request.getPhotoUrl()    != null)
            student.setPhotoUrl(request.getPhotoUrl());
        if (request.getIsActive()    != null)
            //student.setIsActive(request.getIsActive());

        if (request.getAdmissionNo() != null &&
                !request.getAdmissionNo().equals(student.getAdmissionNo())) {
            if (studentRepository.existsBySchoolIdAndAdmissionNo(
                    schoolId, request.getAdmissionNo())) {
                throw new AppException(ErrorCode.ADMISSION_NO_EXISTS);
            }
            student.setAdmissionNo(request.getAdmissionNo());
        }

        return buildFullResponse(studentRepository.save(student), null);
    }

    // ── Enroll student into section ────────────────────────────
    @Transactional
    public EnrollmentDto.Response enrollToSection(
            UUID studentId, StudentDto.EnrollToSectionRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        StudentEnrollment enrollment = enrollIntoSection(
                student, schoolId,
                request.getSectionId(),
                request.getRollNumber(),
                request.getAcademicYearId());

        return mapper.toResponse(enrollment);
    }

    // ── List students in a section ─────────────────────────────
    public List<StudentDto.Response> listBySection(UUID sectionId,
                                                    UUID academicYearId) {
        UUID schoolId = TenantContext.getTenantId();

        // Validate section belongs to this school
        //sectionRepository.findByIdAndSchoolId(sectionId, schoolId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        UUID yearId = academicYearId != null
                ? academicYearId
                : null;//academicYearService.getCurrentYearEntity(schoolId).getId();

        return enrollmentRepository
                .findBySectionAndYear(sectionId, yearId)
                .stream()
                .map(e -> buildFullResponse(e.getStudent(), e))
                .collect(Collectors.toList());
    }

    // ── Add parent to existing student ─────────────────────────
    @Transactional
    public StudentDto.Response addParent(UUID studentId,
                                          ParentDto.AddRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        // Max 2 parents check
        long currentCount = mappingRepository.countByStudent_Id(studentId);
        if (currentCount >= 2) {
            throw new AppException(ErrorCode.PARENT_LIMIT_EXCEEDED);
        }

        Parent parent;

        if (request.getExistingParentId() != null) {
            // Link existing parent
            parent = parentService.getByUserId(request.getExistingParentId());

            if (mappingRepository.existsByStudent_IdAndParent_Id(
                    studentId, parent.getId())) {
                throw new AppException(ErrorCode.PARENT_ALREADY_LINKED);
            }
        } else if (request.getNewParent() != null) {
            // Create new parent and link
            parent = parentService.createParent(schoolId, request.getNewParent());
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Provide either existingParentId or newParent details");
        }

        StudentParentMapping mapping = StudentParentMapping.builder()
                .student(student)
                .parent(parent)
                .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
                .build();

        mappingRepository.save(mapping);

        return buildFullResponse(student, null);
    }

    // ── Private: enroll into section ──────────────────────────
    private StudentEnrollment enrollIntoSection(Student student,
                                                 UUID schoolId,
                                                 UUID sectionId,
                                                 String rollNumber,
                                                 UUID academicYearId) {

        Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        /*AcademicYear year = academicYearId != null
                ? academicYearService.getCurrentYearEntity(schoolId)  // fallback
                : academicYearService.getCurrentYearEntity(schoolId);*/

        var year = academicYearRepository.findBySchoolIdAndIsCurrentTrue(schoolId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "No current academic year configured for this school"));


        // One enrollment per student per academic year
        if (enrollmentRepository.existsByStudent_IdAndAcademicYear_Id(student.getId(), year.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Student is already enrolled for this academic year");
        }

        // Roll number uniqueness check
        if (rollNumber != null &&
                enrollmentRepository.existsBySection_IdAndAcademicYear_IdAndRollNumber(sectionId, year.getId(), rollNumber)) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Roll number '" + rollNumber + "' already assigned in this section");
        }

        StudentEnrollment enrollment = StudentEnrollment.builder()
                //.schoolId(schoolId)
                .student(student)
                .section(section)
                .academicYear(year)
                .rollNumber(rollNumber)
                .build();

        StudentEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student [{}] enrolled in section [{}] for year [{}]",
                student.getFullName(), section.getName(), year.getLabel());

        return saved;
    }

    // ── Private: build full student response ───────────────────
    private StudentDto.Response buildFullResponse(Student student, StudentEnrollment enrollment) {
        //List<StudentParentMapping> mappings = mappingRepository.findByStudent_Id(student.getId());

        StudentDto.Response response = mapper.toResponse(student);
        //response.setParents(mapper.toParentResponses(mappings));

        if (enrollment != null) {
            response.setCurrentEnrollment(mapper.toResponse(enrollment));
        }

        return response;
    }

    // ── Private: get current user ID from security context ─────
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }
}