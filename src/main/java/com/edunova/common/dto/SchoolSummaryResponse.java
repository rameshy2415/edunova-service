package com.edunova.common.dto;

import java.time.Instant;
import java.util.UUID;

public record SchoolSummaryResponse(
        UUID id,
        String  name,
        String  city,
        String  state,
        Boolean isActive,
        String plan,
        String  status,
        Long students,
        Long teachers,
        String admin,
        String  adminEmail,
        Instant createdAt,
        Instant joined,
        Instant renewsOn
) {}
