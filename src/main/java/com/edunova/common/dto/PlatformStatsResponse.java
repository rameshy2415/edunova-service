package com.edunova.common.dto;

import java.math.BigDecimal;

// ── Analytics ──────────────────────────────────────────────────
public record PlatformStatsResponse(
        long    totalSchools,
        long    activeSchools,
        long    trialSchools,
        long    expiredSchools,
        long    totalStudents,
        long    totalTeachers,
        BigDecimal estimatedMrr
) {}
