package com.edunova.module.admin.student.entity;

import com.edunova.module.admin.student.dto.Section;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_enrollments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*@Column(name = "school_id", nullable = false)
    private UUID schoolId;*/

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "roll_number", length = 20)
    private String rollNumber;

   /* @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;*/

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "promoted_from")
    private UUID promotedFrom;         // previous year enrollment id

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime createdAt;
}