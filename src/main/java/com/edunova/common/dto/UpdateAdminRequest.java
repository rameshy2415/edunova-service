package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
public record UpdateAdminRequest(
        @Size(max = 150) String name,
        @Email String email,
        String phone
) {}
