package com.edunova.module.admin.attendance.controller;


import com.edunova.module.admin.attendance.dto.AttendanceDto;
import com.edunova.module.admin.attendance.service.AttendanceService;
import com.edunova.module.admin.student.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ── Get section attendance for a date ─────────────────────
    @GetMapping("")
    @PreAuthorize("hasAnyAuthority('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<AttendanceDto.SectionStudentResponse>> getSectionWiseAttendance() {

        return ResponseEntity.ok(ApiResponse.success(attendanceService.getSectionWiseAttendance()));
    }

    // ── Mark bulk attendance for section ──────────────────────
    @PostMapping("/mark")
    @PreAuthorize("hasAnyAuthority('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<AttendanceDto.SectionAttendanceResponse>>
            markBulk(@Valid @RequestBody AttendanceDto.BulkMarkRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Attendance marked successfully",
                        attendanceService.markBulk(request)));
    }

    // ── Get section attendance for a date ─────────────────────
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyAuthority('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<AttendanceDto.SectionAttendanceResponse>>
            getSectionAttendance(
                    @PathVariable UUID sectionId,
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        attendanceService.getSectionAttendance(sectionId, date)));
    }

    // ── Get section attendance date range ─────────────────────
    @GetMapping("/section/{sectionId}/range")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<AttendanceDto.Response>>>
            getSectionRange(
                    @PathVariable UUID sectionId,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        attendanceService.getSectionAttendanceRange(
                                sectionId, from, to)));
    }

    // ── Get student attendance date range ─────────────────────
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<List<AttendanceDto.Response>>>
            getStudentAttendance(
                    @PathVariable UUID studentId,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        attendanceService.getStudentAttendance(
                                studentId, from, to)));
    }

    // ── Get student attendance summary ────────────────────────
    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<AttendanceDto.StudentSummary>>
            getStudentSummary(
                    @PathVariable UUID studentId,
                    @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        attendanceService.getStudentSummary(
                                studentId, academicYearId)));
    }

    // ── Update single attendance record ───────────────────────
    @PatchMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<AttendanceDto.Response>> update(
            @PathVariable UUID attendanceId,
            @Valid @RequestBody AttendanceDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Attendance updated",
                        attendanceService.updateAttendance(
                                attendanceId, request)));
    }

    // ── Parent: view child's attendance ───────────────────────
    @GetMapping("/my-child/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<List<AttendanceDto.Response>>>
            myChildAttendance(
                    @PathVariable UUID studentId,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                    @RequestParam
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        attendanceService.getMyChildAttendance(
                                studentId, from, to)));
    }
}