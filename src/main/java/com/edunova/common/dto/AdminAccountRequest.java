package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AdminAccountRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 20) String phone,
        boolean sendWelcomeEmail
) {}
