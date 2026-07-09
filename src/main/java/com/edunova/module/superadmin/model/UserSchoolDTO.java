package com.edunova.module.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSchoolDTO {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    private UUID schoolId;
    private String schoolName;
}
