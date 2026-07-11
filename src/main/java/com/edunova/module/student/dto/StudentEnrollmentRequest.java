package com.edunova.module.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollmentRequest {

    // Student Details
    @NotBlank(message = "Student name is required")
    @Size(max = 150)
    private String name;

    private LocalDate dateOfBirth;

    @NotBlank
    private String gender;

    @Size(max = 5)
    private String bloodGroup;

    private String roll;

    //@NotBlank
    private UUID section;

    //@NotBlank
    private UUID schoolId;

    //@NotBlank
    private UUID academicYearId;

   // @NotBlank
    private String status;

   // @NotBlank
    private String fees;

    //@NotBlank
    private String nationality;

    private String religion;

    private String category;

    private String house;

    private String previousSchool;

    // Parent / Guardian Details
    private String father;

    private String mother;

    private String aadhaarNo;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Phone number must be 10 digits"
    )
    private String phone;

    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Alternate phone number must be 10 digits"
    )
    private String altPhone;

    @Email(message = "Invalid email address")
    private String email;

    @Size(max = 500)
    private String address;

    private String emergencyContact;
}
