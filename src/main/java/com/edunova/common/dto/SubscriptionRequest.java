package com.edunova.common.dto;

import com.edunova.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.UUID;
public record SubscriptionRequest(
        UUID planId,
        String plan,
        BillingCycle billingCycle,
        BigDecimal amountOverride,
        @DecimalMin("0") @DecimalMax("100") BigDecimal discountPct,
        Integer trialDays,              // 0 = no trial
        String notes
) {}