package com.edunova.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

// ── Subscription plan ──────────────────────────────────────────
public record SubscriptionPlanResponse(
        UUID id,
        String     name,
        Integer    maxStudents,
        Integer    maxTeachers,
        BigDecimal priceMonthly,
        BigDecimal priceAnnually,
        String     features,
        Boolean    isActive
) {}
