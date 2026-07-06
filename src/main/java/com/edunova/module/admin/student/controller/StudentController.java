package com.edunova.module.admin.student.controller;

import com.edunova.module.admin.student.dto.*;
import com.edunova.module.admin.student.dto.ApiResponse;
import com.edunova.module.admin.student.entity.Student;
import com.edunova.module.admin.student.service.ParentService;
import com.edunova.module.admin.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.edunova.common.dto.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
@Validated
public class StudentController {

    private final StudentService studentService;
    private final ParentService parentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<com.edunova.module.admin.student.dto.ApiResponse<StudentDto.Response>> enroll(@Valid @RequestBody StudentEnrollmentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.edunova.module.admin.student.dto.ApiResponse.success("Student enrolled successfully", studentService.enroll(request)));
    }

    @GetMapping("/list/{schoolId}")
    @PreAuthorize("hasAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<List<StudentResponseDTO>> studentsList(@PathVariable UUID schoolId) {

        return ResponseEntity.status(HttpStatus.OK).body(studentService.getBySchoolId(schoolId));
    }

    // ── Get student by ID ──────────────────────────────────────
    @GetMapping("/{studentId}")
    @PreAuthorize("hasAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<StudentDto.StudentResponse>> getById(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getById(studentId)));
    }

    // ── Update student ─────────────────────────────────────────
    @PatchMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Student updated",
                        studentService.update(studentId, request)));
    }


    @GetMapping("/{schoolId}/grades")
    @PreAuthorize("hasAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<StudentDto.Grade>> getGrade(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(ApiResponse.success(studentService.getGradesBySchoolId(schoolId)));
    }

   /*

    // ── Search students ────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN')")
    public ResponseEntity<ApiResponse<Page<StudentDto.Response>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        studentService.search(search, isActive, pageable)));
    }

    // ── Update student ─────────────────────────────────────────
    @PatchMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Student updated",
                        studentService.update(studentId, request)));
    }

    // ── Enroll student into section ────────────────────────────
    @PostMapping("/{studentId}/enroll")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<EnrollmentDto.Response>> enrollToSection(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDto.EnrollToSectionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Student enrolled into section",
                        studentService.enrollToSection(studentId, request)));
    }

    // ── List students in a section ─────────────────────────────
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<StudentDto.Response>>> listBySection(
            @PathVariable UUID sectionId,
            @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        studentService.listBySection(sectionId, academicYearId)));
    }

    // ── Add parent to existing student ─────────────────────────
    @PostMapping("/{studentId}/parents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> addParent(
            @PathVariable UUID studentId,
            @Valid @RequestBody ParentDto.AddRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Parent added successfully",
                        studentService.addParent(studentId, request)));
    }

    // ── Parent dashboard — view own children ───────────────────
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentDto.Response>> myChildren(
            @AuthenticationPrincipal UUID currentUserId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        parentService.getParentDashboard(currentUserId)));
    }*/
}