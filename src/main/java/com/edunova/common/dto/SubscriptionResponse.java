package com.edunova.common.dto;

import com.edunova.enums.BillingCycle;
import com.edunova.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// ── Subscription ───────────────────────────────────────────────
public record SubscriptionResponse(
        UUID id,
        UUID               schoolId,
        String             schoolName,
        SubscriptionPlanResponse plan,
        String status,
        BillingCycle billingCycle,
        BigDecimal amountOverride,
        BigDecimal         discountPct,
        BigDecimal         effectiveMonthlyAmount,
        Instant trialEndsAt,
        LocalDate          currentPeriodStart,
        LocalDate currentPeriodEnd,
        Instant            cancelledAt,
        String             cancellationReason,
        String             internalNotes,
        Instant            createdAt,
        Instant            updatedAt
) {}
