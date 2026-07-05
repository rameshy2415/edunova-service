package com.edunova.module.admin.student.repository;

import com.edunova.module.admin.student.dto.StudentResponseDTO;
import com.edunova.module.admin.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndSchoolId(UUID id, UUID schoolId);

    @Query("""
               SELECT new com.edunova.module.admin.student.dto.StudentResponseDTO(
                   s.id,
                   s.schoolId,
                   s.admissionNo,
                   s.name,
                   s.dateOfBirth,
                   s.gender,
                   s.bloodGroup,
                   s.nationality,
                   s.religion,
                   s.category,
                   s.motherTongue,
                   s.aadhaarNo,
                   s.photoUrl,
                   s.status,
                   s.address,
                   s.previousSchool,
                   s.admissionDate,
                   s.leavingDate,
                   s.leavingReason,
                   s.notes,
                   s.createdAt,
                   s.updatedAt,
                   s.isActive,
                   g.id,
                   sec.id,
                   se.rollNumber,
                   sg.father,
                   sg.mother,
                   sg.phone,
                   sg.altPhone,
                   sg.email,
                   sg.address,
                   sg.emergencyContact
               ) FROM StudentEnrollment se
                 JOIN se.student s
                 JOIN se.section sec
                 JOIN sec.grade g
                 LEFT JOIN StudentGuardian sg ON sg.studentId = s.id
                 WHERE s.id = :id
            """)
    Optional<StudentResponseDTO> findByStudentId(@Param("id") UUID id);


    @Query("""
               SELECT new com.edunova.module.admin.student.dto.StudentResponseDTO(
                   s.id,
                   s.schoolId,
                   s.admissionNo,
                   s.name,
                   s.dateOfBirth,
                   s.gender,
                   s.bloodGroup,
                   s.nationality,
                   s.religion,
                   s.category,
                   s.motherTongue,
                   s.aadhaarNo,
                   s.photoUrl,
                   s.status,
                   s.address,
                   s.previousSchool,
                   s.admissionDate,
                   s.leavingDate,
                   s.leavingReason,
                   s.notes,
                   s.createdAt,
                   s.updatedAt,
                   s.isActive,
                   g.id,
                   sec.id,
                   se.rollNumber,
                   sg.father,
                   sg.mother,
                   sg.phone,
                   sg.altPhone,
                   sg.email,
                   sg.address,
                   sg.emergencyContact
               ) FROM StudentEnrollment se
                 JOIN se.student s
                 JOIN se.section sec
                 JOIN sec.grade g
                 LEFT JOIN StudentGuardian sg ON sg.studentId = s.id
                 WHERE s.schoolId = :schoolId
                 ORDER BY s.name ASC
            """)
    List<StudentResponseDTO> findBySchoolId(@Param("schoolId") UUID schoolId);

    boolean existsBySchoolIdAndAdmissionNo(UUID schoolId, String admissionNo);

    // Search students by name or admission number
   /* @Query("""
        SELECT s FROM Student s
        WHERE s.schoolId = :schoolId
        AND s.isActive = :isActive
        AND (
            :search IS NULL
            OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY s.firstName ASC, s.lastName ASC
    """)
    Page<Student> searchStudents(UUID schoolId,
                                  Boolean isActive,
                                  String search,
                                  Pageable pageable);*/

    @Query("""
                SELECT s FROM Student s
                WHERE s.schoolId = :schoolId
                AND (
                    :search IS NULL
                    OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                ORDER BY s.name ASC
            """)
    Page<Student> searchStudents(UUID schoolId,
                                 Boolean isActive,
                                 String search,
                                 Pageable pageable);

    // Count active students per school
    long countBySchoolIdAndIsActiveTrue(UUID schoolId);

    // Latest admission number for auto-generation
    @Query("""
                SELECT s.admissionNo FROM Student s
                WHERE s.schoolId = :schoolId
                AND s.admissionNo IS NOT NULL
                ORDER BY s.createdAt DESC
                LIMIT 1
            """)
    Optional<String> findLatestAdmissionNo(UUID schoolId);
}