package com.edunova.common.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RenewSubscriptionRequest(
        @NotNull LocalDate newPeriodEnd,
        String notes
) {}
