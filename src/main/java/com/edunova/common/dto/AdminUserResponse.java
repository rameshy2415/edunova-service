package com.edunova.common.dto;

import java.time.Instant;
import java.util.UUID;

// ── Admin user ─────────────────────────────────────────────────
public record AdminUserResponse(
        UUID id,
        String  name,
        String  email,
        String  phone,
        UUID    schoolId,
        String  schoolName,
        Boolean isActive,
        Instant lastLoginAt,
        Instant createdAt
) {}
