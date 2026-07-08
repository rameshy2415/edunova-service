package com.edunova.module.admin.attendance.repository;


import com.edunova.module.admin.attendance.dto.AttendanceSectionDto;
import com.edunova.module.admin.attendance.entity.Attendance;
import com.edunova.module.admin.student.dto.StudentResponseDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    // Single student attendance for a date
    Optional<Attendance> findByStudent_IdAndDate(UUID studentId, LocalDate date);

    // All attendance for a section on a date
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.student s
        WHERE a.section.id = :sectionId
        AND a.date = :date
        ORDER BY s.name ASC
    """)
    List<Attendance> findBySectionAndDate(UUID sectionId, LocalDate date);

    // Student attendance between two dates
    @Query("""
        SELECT a FROM Attendance a
        WHERE a.student.id = :studentId
        AND a.date BETWEEN :from AND :to
        ORDER BY a.date ASC
    """)
    List<Attendance> findByStudentAndDateRange(UUID studentId,
                                               LocalDate from,
                                               LocalDate to);

    // Section attendance between two dates
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.student s
        WHERE a.section.id = :sectionId
        AND a.date BETWEEN :from AND :to
        ORDER BY a.date ASC, s.name ASC
    """)
    List<Attendance> findBySectionAndDateRange(UUID sectionId,
                                               LocalDate from,
                                               LocalDate to);

    // Count by status for a student in date range (for reports)
    @Query("""
        SELECT a.status, COUNT(a)
        FROM Attendance a
        WHERE a.student.id = :studentId
        AND a.academicYear.id = :academicYearId
        GROUP BY a.status
    """)
    List<Object[]> countByStatusForStudent(UUID studentId, UUID academicYearId);

    // Check if attendance already marked for section on date
    boolean existsBySection_IdAndDate(UUID sectionId, LocalDate date);

    // All absent students in a school on a date (for notifications)
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.student s
        WHERE a.schoolId = :schoolId
        AND a.date = :date
        AND a.status = 'ABSENT'
    """)
    List<Attendance> findAbsentStudents(UUID schoolId, LocalDate date);

    // Monthly summary per student
    @Query("""
        SELECT a.date, a.status
        FROM Attendance a
        WHERE a.student.id = :studentId
        AND YEAR(a.date) = :year
        AND MONTH(a.date) = :month
        ORDER BY a.date ASC
    """)
    List<Object[]> findMonthlyAttendance(UUID studentId, int year, int month);


    @Query("""
               SELECT new com.edunova.module.admin.attendance.dto.AttendanceSectionDto(
                   s.id,
                   s.name,
                   sec.id,
                   sec.displayName,
                   se.rollNumber
               ) FROM StudentEnrollment se
                 JOIN se.student s
                 JOIN se.section sec
                 JOIN sec.grade g
                 LEFT JOIN StudentGuardian sg ON sg.studentId = s.id
                 WHERE s.schoolId = :schoolId and se.isActive = true
                 ORDER BY s.name ASC
            """)
    List<AttendanceSectionDto> findStudentBySchoolId(@Param("schoolId") UUID schoolId);
}