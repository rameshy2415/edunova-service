package com.edunova.module.admin.attendance.entity;


import com.edunova.module.admin.student.dto.Section;
import com.edunova.module.admin.student.entity.AcademicYear;
import com.edunova.module.admin.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status;

    @Column(name = "marked_by")
    private UUID markedBy;

    @Column(name = "note",length = 255)
    private String remarks;

    @CreationTimestamp
    @Column(name = "marked_at", updatable = false)
    private LocalDateTime markedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Status enum ───────────────────────────────────────────
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        HOLIDAY,
        LEAVE
    }
}