package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;
// ── Create / update admin ─────────────────────────────────────
public record CreateAdminRequest(
        @NotNull UUID schoolId,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank @Size(min = 8) String tempPassword,
        boolean sendWelcomeEmail
) {}
