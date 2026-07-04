package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// ── Update school ──────────────────────────────────────────────
public record UpdateSchoolRequest(
        @Size(max = 200) String name,
        String board,
        String address,
        String city,
        String state,
        String pincode,
        String phone,
        @Email String email,
        String website,
        String principalName,
        Integer establishedYear,
        String affiliationNo,
        Boolean isActive
) {}
