package com.edunova.module.admin.student.service;


import com.edunova.exception.AppException;
import com.edunova.exception.ErrorCode;
import com.edunova.module.admin.student.dto.ParentDto;
import com.edunova.module.admin.student.dto.Role;
import com.edunova.module.admin.student.entity.Parent;
import com.edunova.module.admin.student.mapper.StudentMapper;
import com.edunova.module.admin.student.repository.ParentRepository;
import com.edunova.module.admin.student.repository.RoleRepository;
import com.edunova.module.admin.student.repository.StudentEnrollmentRepository;
import com.edunova.module.admin.student.repository.StudentParentMappingRepository;
import com.edunova.module.superadmin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentParentMappingRepository mappingRepository;
    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentMapper mapper;

    // ── Create parent user + parent record ────────────────────
    @Transactional
    public Parent createParent(UUID schoolId,
                               ParentDto.CreateRequest request) {

        // At least one contact required
        if (request.getEmail() == null && request.getMobile() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Parent must have at least one contact — email or mobile");
        }

        // Check email uniqueness if provided
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Email '" + request.getEmail() + "' is already registered");
        }

        // Check mobile uniqueness if provided
        if (request.getMobile() != null &&
                userRepository.existsByMobile(request.getMobile())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Mobile '" + request.getMobile() + "' is already registered");
        }

        Role parentRole = roleRepository.findByName(Role.PARENT)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR,
                        "PARENT role not found"));

        // Create user account for parent (OTP login)
        /*User1 user1 = User1.builder()
                .schoolId(schoolId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .authType(User1.AuthType.OTP)
                .isActive(true)
                .roles(Set.of(parentRole))
                .build();*/

        //userRepository.save(user);

        // Create parent record
        Parent parent = Parent.builder()
                .schoolId(schoolId)
                .user(null)
                .relation(request.getRelation() != null
                        ? request.getRelation().toUpperCase() : "GUARDIAN")
                .occupation(request.getOccupation())
                .build();

        Parent saved = parentRepository.save(parent);
        //log.info("Parent created: {} [{}]", user1.getFullName(), saved.getId());
        log.info("Parent created:  [{}]", saved.getId());
        return saved;
    }

    // ── Get parent dashboard — linked students ─────────────────
    public ParentDto.Response getParentDashboard(UUID userId) {

        Parent parent = parentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARENT_NOT_FOUND));

        // Get all linked students with their current enrollment
        List<ParentDto.StudentSummary> studentSummaries =
                mappingRepository.findStudentsByParentId(parent.getId())
                        .stream()
                        .map(m -> {
                            // Try to get current enrollment for grade/section info
                            var enrollment = enrollmentRepository
                                    .findByStudent_IdAndAcademicYear_Id(
                                            m.getStudent().getId(),
                                            getCurrentYearId(parent.getSchoolId()))
                                    .orElse(null);
                            return mapper.toStudentSummary(m.getStudent(), enrollment);
                        })
                        .collect(Collectors.toList());

        return ParentDto.Response.builder()
                .id(parent.getId())
                .firstName(parent.getUser().getFirstName())
                .lastName(parent.getUser().getLastName())
                .fullName(parent.getFullName())
                .email(parent.getEmail())
                .mobile(parent.getMobile())
                .relation(parent.getRelation())
                .linkedStudents(studentSummaries)
                .build();
    }

    // ── Helper ─────────────────────────────────────────────────
    private UUID getCurrentYearId(UUID schoolId) {
        // Returns null if no current year set — enrollment lookup returns null
        try {
            return parentRepository
                    .findById(schoolId)
                    .map(p -> p.getSchoolId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Get parent by user ID (for auth context) ───────────────
    public Parent getByUserId(UUID userId) {
        return parentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARENT_NOT_FOUND));
    }
}