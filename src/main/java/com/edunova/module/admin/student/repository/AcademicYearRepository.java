package com.edunova.module.admin.student.repository;


import com.edunova.module.admin.student.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    @Query("""
            select a from AcademicYear a where a.schoolId =:schoolId and a.isCurrent = true
            """)
    Optional<AcademicYear> findBySchoolIdAndIsCurrentTrue(UUID schoolId);

    @Query("""
            select a.id from AcademicYear a where a.schoolId =:schoolId and a.isCurrent = true
            """)
    UUID findIdBySchoolIdAndIsCurrentTrue(UUID schoolId);

    boolean existsBySchoolIdAndLabel(UUID schoolId, String label);

    // Unset all current flags before setting a new one
    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.schoolId = :schoolId")
    void unsetCurrentForSchool(UUID schoolId);
}