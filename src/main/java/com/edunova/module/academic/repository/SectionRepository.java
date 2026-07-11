package com.edunova.module.academic.repository;


import com.edunova.module.academic.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByGrade_IdAndIsActiveTrueOrderByNameAsc(UUID gradeId);

    List<Section> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    Optional<Section> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByGrade_IdAndNameIgnoreCase(UUID gradeId, String name);

/*    // All sections for a school with grade info (for listing)
    @Query("""
        SELECT s FROM Section s
        JOIN FETCH s.grade g
        WHERE s.schoolId = :schoolId
        AND s.isActive = true
        ORDER BY g.order ASC, s.name ASC
    """)
    List<Section> findAllWithGradeBySchoolId(UUID schoolId);*/

    @Query("""
                SELECT DISTINCT s
                FROM StudentEnrollment se
                JOIN se.section s
                JOIN FETCH s.grade g
                WHERE s.schoolId = :schoolId
                  AND s.isActive = true
                  AND se.isActive = true
                ORDER BY g.order ASC, s.name ASC
            """)
    List<Section> findSectionsWithStudents(UUID schoolId);

    @Query("""
                SELECT DISTINCT s
                FROM StudentEnrollment se
                JOIN se.section s
                JOIN FETCH s.grade g
                WHERE s.schoolId = :schoolId
                  AND se.academicYear.id = :academicYearId
                  AND s.isActive = true
                  AND se.isActive = true
                ORDER BY g.order ASC, s.name ASC
            """)
    List<Section> findSectionsWithStudents(UUID schoolId, UUID academicYearId);//TODO will be using once academicYearId Is integrated


    // All sections for a school with grade info (for listing)
    @Query("""
        SELECT s FROM Section s
        JOIN FETCH s.grade g
        WHERE s.schoolId = :schoolId
        AND s.isActive = true
        ORDER BY g.order ASC, s.name ASC
    """)
    List<com.edunova.module.academic.entity.Section> findAllWithGradeBySchoolId(UUID schoolId);
}