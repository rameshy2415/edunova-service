package com.edunova.module.academic.repository;


import com.edunova.module.academic.entity.Grade;
import com.edunova.module.student.dto.GradeDTO;
import com.edunova.module.student.dto.GradeSectionDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    // List<AcademicYear> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    @Query("""
            SELECT new com.edunova.module.student.dto.GradeDTO(
                g.id,
                g.name,
                g.displayName
            )
            FROM Grade g
            WHERE g.schoolId = :schoolId
            ORDER BY g.order
            """)
    List<GradeDTO> findAllGradeBySchoolId(@Param("schoolId") UUID schoolId);

    @Query("""
            SELECT new com.edunova.module.student.dto.GradeSectionDTO(
                g.id,
                g.name,
                g.displayName,
                g.order,
                s.id,
                s.name,
                s.displayName
            )
            FROM Grade g
            LEFT JOIN Section s
                   ON s.grade = g
            WHERE g.schoolId = :schoolId
            AND g.isActive = true
            AND (s IS NULL OR s.isActive = true)
            ORDER BY g.order, s.displayName
            """)
    List<GradeSectionDTO> findGradesWithSections(UUID schoolId);



    /*boolean existsBySchoolIdAndLabel(UUID schoolId, String label);

    // Unset all current flags before setting a new one
    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.schoolId = :schoolId")
    void unsetCurrentForSchool(UUID schoolId);*/

    List<Grade> findBySchoolIdAndIsActiveTrueOrderByOrderAsc(UUID schoolId);

    List<Grade> findBySchoolIdOrderByOrderAsc(UUID schoolId);

    Optional<Grade> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);

    boolean existsBySchoolIdAndOrder(UUID schoolId, Integer order);
}