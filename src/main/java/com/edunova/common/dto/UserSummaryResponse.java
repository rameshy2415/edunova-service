package com.edunova.common.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String name,
        String email,
        String role,
        UUID schoolId,
        String schoolName,
        String avatarUrl,
        boolean isFirstTime,
        LocalDateTime expiresAt
) {}
