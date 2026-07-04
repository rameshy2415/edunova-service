package com.edunova.common.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelSubscriptionRequest(
        @NotBlank String reason
) {}
