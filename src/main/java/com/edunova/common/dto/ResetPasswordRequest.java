package com.edunova.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// ── Reset password ─────────────────────────────────────────────
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword,
        @NotBlank @Size(min = 8) String confirmPassword
) {}
