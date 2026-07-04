package com.edunova.module.admin.student.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "student_guardians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /*@OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;*/

    @Column(name = "father_name", length = 150)
    private String father;

    @Column(name = "mother_name", length = 150)
    private String mother;

    @Column(name = "phone", length = 20, nullable = false)
    private String phone;

    @Column(name = "alternate_phone", length = 20)
    private String altPhone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "emergency_contact", length = 150)
    private String emergencyContact;
}
