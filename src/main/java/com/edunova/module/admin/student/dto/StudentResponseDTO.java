package com.edunova.module.admin.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponseDTO {

    private UUID id;
    private UUID schoolId;
    private String admissionNo;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    //@Builder.Default
    private String nationality ;
    private String religion;
    //@Builder.Default
    private String category ;
    private String motherTongue;
    private String aadhaarNo;
    private String photoUrl;
    //@Builder.Default
    private String status;// = "Active";
    private String address;
    private String previousSchool;
    //@Builder.Default
    private LocalDate admissionDate;// = LocalDate.now();
    private LocalDate leavingDate;
    private String leavingReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    //@Builder.Default
    private Boolean isActive;// = true;
    //Enrolls details
    private UUID grade;
    private UUID sectionId;
    private String section;

    private String roll;
    private String father;
    private String mother;
    private String phone;
    private String altPhone;
    private String email;
    private String guardianAddress;
    private String emergencyContact;

    public StudentResponseDTO(UUID id, UUID schoolId, String admissionNo, String name, LocalDate dateOfBirth, String gender, String bloodGroup, String nationality, String religion, String category, String motherTongue, String aadhaarNo, String photoUrl, String status, String address, String previousSchool, LocalDate admissionDate, LocalDate leavingDate, String leavingReason, String notes, LocalDateTime createdAt, LocalDateTime updatedAt, Boolean isActive, UUID grade,UUID sectionId,String section, String roll, String father, String mother, String phone, String altPhone, String email, String guardianAddress, String emergencyContact) {
        this.id = id;
        this.schoolId = schoolId;
        this.admissionNo = admissionNo;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.bloodGroup = bloodGroup;
        this.nationality = nationality;
        this.religion = religion;
        this.category = category;
        this.motherTongue = motherTongue;
        this.aadhaarNo = aadhaarNo;
        this.photoUrl = photoUrl;
        this.status = status;
        this.address = address;
        this.previousSchool = previousSchool;
        this.admissionDate = admissionDate;
        this.leavingDate = leavingDate;
        this.leavingReason = leavingReason;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.grade = grade;
        this.sectionId = sectionId;
        this.section = section;
        this.roll = roll;
        this.father = father;
        this.mother = mother;
        this.phone = phone;
        this.altPhone = altPhone;
        this.email = email;
        this.guardianAddress = guardianAddress;
        this.emergencyContact = emergencyContact;
        this.attendance = 96;
        this.fees = "Partial";
    }

    @Builder.Default
    private Integer attendance = 96;

    @Builder.Default
    private String fees="Partial";

}
