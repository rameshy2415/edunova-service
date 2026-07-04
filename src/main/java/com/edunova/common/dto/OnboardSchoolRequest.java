package com.edunova.common.dto;


import jakarta.validation.Valid;
// ── Onboard school (3-part payload) ───────────────────────────
public record OnboardSchoolRequest(
        @Valid SchoolInfoRequest    school,
        @Valid SubscriptionRequest  subscription,
        @Valid AdminAccountRequest  admin
) {}


