package com.edunova.module.student.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "admission_no", nullable = false, length = 30)
    private String admissionNo;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 50)
    private String gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Builder.Default
    @Column(nullable = false, length = 60)
    private String nationality = "Indian";

    @Column(length = 60)
    private String religion;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String category = "General";

    @Column(name = "mother_tongue", length = 60)
    private String motherTongue;

    @Column(name = "aadhaar_no", length = 12)
    private String aadhaarNo;

    @Column(name = "photo_url")
    private String photoUrl;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "Active";

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "previous_school", length = 200)
    private String previousSchool;

    @Builder.Default
    @Column(name = "admission_date", nullable = false)
    private LocalDate admissionDate = LocalDate.now();

    @Column(name = "leaving_date")
    private LocalDate leavingDate;

    @Column(name = "leaving_reason", columnDefinition = "TEXT")
    private String leavingReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ── helper ────────────────────────────────────────────────
    public String getFullName() {
        return name;
    }
}