package com.edunova.module.admin.attendance.service;


import com.edunova.config.TenantContext;
import com.edunova.exception.AppException;
import com.edunova.exception.ErrorCode;
import com.edunova.filter.LoggedInUserContextDetails;
import com.edunova.module.admin.attendance.dto.AttendanceDto;
import com.edunova.module.admin.attendance.dto.AttendanceSectionDto;
import com.edunova.module.admin.attendance.dto.IdNameDto;
import com.edunova.module.admin.attendance.entity.Attendance;
import com.edunova.module.admin.attendance.mapper.AttendanceMapper;
import com.edunova.module.admin.attendance.repository.AttendanceRepository;
import com.edunova.module.admin.student.dto.Section;
import com.edunova.module.admin.student.entity.AcademicYear;
import com.edunova.module.admin.student.entity.Student;
import com.edunova.module.admin.student.repository.SectionRepository;
import com.edunova.module.admin.student.repository.StudentEnrollmentRepository;
import com.edunova.module.admin.student.repository.StudentRepository;
import com.edunova.module.admin.student.service.AcademicYearService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final AcademicYearService academicYearService;
    private final AttendanceMapper mapper;

    // ── Mark bulk attendance for section ──────────────────────
    @Transactional
    public AttendanceDto.SectionAttendanceResponse markBulk(
            AttendanceDto.BulkMarkRequest request) {

        UUID schoolId = TenantContext.getTenantId();
        UUID markedBy = getCurrentUserId();

        // Validate date is not in future
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot mark attendance for a future date");
        }

        // Validate date is not a Sunday
        if (request.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot mark attendance on Sunday");
        }

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = null;//academicYearService.getCurrentYearEntity(schoolId);//TODO

        // Validate date falls within academic year
        if (request.getDate().isBefore(year.getStartDate()) ||
                request.getDate().isAfter(year.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Date is outside the current academic year");
        }

        List<Attendance> saved = new ArrayList<>();

        for (AttendanceDto.StudentAttendance record : request.getRecords()) {

            Student student = studentRepository
                    .findByIdAndSchoolId(record.getStudentId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND,
                            "Student not found: " + record.getStudentId()));

            // Upsert — update if already exists for this date
            Attendance attendance = attendanceRepository
                    .findByStudent_IdAndDate(student.getId(), request.getDate())
                    .orElse(Attendance.builder()
                            .schoolId(schoolId)
                            .student(student)
                            .section(section)
                            .academicYear(year)
                            .date(request.getDate())
                            .build());

            attendance.setStatus(record.getStatus());
            attendance.setRemarks(record.getRemarks());
            attendance.setMarkedBy(markedBy);

            saved.add(attendanceRepository.save(attendance));
        }

        log.info("Attendance marked for section [{}] date [{}] — {} records",
                section.getName(), request.getDate(), saved.size());

        return buildSectionResponse(section, request.getDate(), saved);
    }

    // ── Get section attendance for a date ─────────────────────
    public AttendanceDto.SectionAttendanceResponse getSectionAttendance(
            UUID sectionId, LocalDate date) {

        UUID schoolId = TenantContext.getTenantId();

        Section section = sectionRepository
                .findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<Attendance> records = attendanceRepository
                .findBySectionAndDate(sectionId, targetDate);

        return buildSectionResponse(section, targetDate, records);
    }

    // ── Get section attendance for a date ─────────────────────
    public AttendanceDto.SectionStudentResponse getSectionWiseAttendance() {

        var userContextDetails= LoggedInUserContextDetails.getCurrentUser();
        var schoolId = userContextDetails.getSchoolId();
        if(schoolId == null){
            throw  new AppException(ErrorCode.NOT_FOUND, "School Id not found");
        }

        //UUID schoolId = TenantContext.getTenantId();

       // Section section = sectionRepository.findByIdAndSchoolId(sectionId, schoolId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Section not found"));

        //LocalDate targetDate = date != null ? date : LocalDate.now();//TODO need to revisit
        LocalDate targetDate =LocalDate.now();

        List<AttendanceSectionDto> records = attendanceRepository.findStudentBySchoolId(schoolId);

        List<IdNameDto> sections = records.stream()
                .collect(Collectors.toMap(
                        AttendanceSectionDto::getSectionId,
                        AttendanceSectionDto::getSectionName,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .entrySet()
                .stream()
                .map(entry -> new IdNameDto(entry.getKey(), entry.getValue()))
                .toList();

        return AttendanceDto.SectionStudentResponse.builder()
                .students(records)
                .section(sections)
                .build();

        //return buildSectionResponse(section, targetDate, records);
    }

    // ── Get attendance sheet for section (date range) ─────────
    public List<AttendanceDto.Response> getSectionAttendanceRange(
            UUID sectionId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();

        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        validateDateRange(from, to);

        return attendanceRepository
                .findBySectionAndDateRange(sectionId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get student attendance (date range) ───────────────────
    public List<AttendanceDto.Response> getStudentAttendance(
            UUID studentId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();

        studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateDateRange(from, to);

        return attendanceRepository
                .findByStudentAndDateRange(studentId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get student attendance summary ────────────────────────
    public AttendanceDto.StudentSummary getStudentSummary(
            UUID studentId, UUID academicYearId) {

        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        UUID yearId = null;
        //UUID yearId = academicYearId != null ? academicYearId : academicYearService.getCurrentYearEntity(schoolId).getId();//TODO

        // Get status counts
        List<Object[]> counts = attendanceRepository
                .countByStatusForStudent(studentId, yearId);

        Map<Attendance.AttendanceStatus, Integer> statusMap = new HashMap<>();
        int total = 0;

        for (Object[] row : counts) {
            Attendance.AttendanceStatus status = (Attendance.AttendanceStatus) row[0];
            int count = ((Long) row[1]).intValue();
            statusMap.put(status, count);
            if (status != Attendance.AttendanceStatus.HOLIDAY) {
                total += count;
            }
        }

        int present  = statusMap.getOrDefault(Attendance.AttendanceStatus.PRESENT,  0);
        int absent   = statusMap.getOrDefault(Attendance.AttendanceStatus.ABSENT,   0);
        int late     = statusMap.getOrDefault(Attendance.AttendanceStatus.LATE,     0);
        int leave    = statusMap.getOrDefault(Attendance.AttendanceStatus.LEAVE,    0);

        double percentage = total > 0
                ? Math.round(((present + late) * 100.0 / total) * 100.0) / 100.0
                : 0.0;

        return AttendanceDto.StudentSummary.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .admissionNo(student.getAdmissionNo())
                .totalWorkingDays(total)
                .presentDays(present)
                .absentDays(absent)
                .lateDays(late)
                .leaveDays(leave)
                .attendancePercentage(percentage)
                .build();
    }

    // ── Update single student attendance ──────────────────────
    @Transactional
    public AttendanceDto.Response updateAttendance(UUID attendanceId,
                                                    AttendanceDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Attendance attendance = attendanceRepository
                .findById(attendanceId)
                .filter(a -> a.getSchoolId().equals(schoolId))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Attendance record not found"));

        attendance.setStatus(request.getStatus());
        if (request.getRemarks() != null)
            attendance.setRemarks(request.getRemarks());
        attendance.setMarkedBy(getCurrentUserId());

        return mapper.toResponse(attendanceRepository.save(attendance));
    }

    // ── Parent: get own child's attendance ────────────────────
    public List<AttendanceDto.Response> getMyChildAttendance(
            UUID studentId, LocalDate from, LocalDate to) {

        UUID schoolId = TenantContext.getTenantId();
        UUID userId   = getCurrentUserId();

        // Verify this student is linked to the logged-in parent
        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        validateDateRange(from, to);

        return attendanceRepository
                .findByStudentAndDateRange(studentId, from, to)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Private: build section attendance response ─────────────
    private AttendanceDto.SectionAttendanceResponse buildSectionResponse(
            Section section,
            LocalDate date,
            List<Attendance> records) {

        List<AttendanceDto.Response> responseList = records.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        long presentCount = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long absentCount  = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
        long lateCount    = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE).count();
        long leaveCount   = records.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LEAVE).count();

        return AttendanceDto.SectionAttendanceResponse.builder()
                .sectionId(section.getId())
                .sectionName(section.getName())
                .gradeName(section.getGrade().getName())
                .date(date)
                .alreadyMarked(!records.isEmpty())
                .totalStudents(records.size())
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .lateCount((int) lateCount)
                .leaveCount((int) leaveCount)
                .records(responseList)
                .build();
    }

    // ── Private: validate date range ──────────────────────────
    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "From date must be before to date");
        }
        if (from.plusDays(90).isBefore(to)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Date range cannot exceed 90 days");
        }
    }

    // ── Private: get current user from security context ────────
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }
}