package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolInfoRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank String board,
        String address,
        @NotBlank String city,
        @NotBlank String state,
        String pincode,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Email String email,
        String website,
        String principalName,
        Integer establishedYear,
        String affiliationNo
) {}
