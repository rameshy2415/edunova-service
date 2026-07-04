package com.edunova.common.dto;

import com.edunova.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.UUID;
// ── Update subscription ────────────────────────────────────────
public record UpdateSubscriptionRequest(
        UUID planId,
        BillingCycle billingCycle,
        BigDecimal amountOverride,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPct,
        String notes
) {}
