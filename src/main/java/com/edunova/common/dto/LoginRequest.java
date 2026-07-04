package com.edunova.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ── Login ──────────────────────────────────────────────────────
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
        //@NotBlank String role       // sent from frontend to disambiguate
) {}
