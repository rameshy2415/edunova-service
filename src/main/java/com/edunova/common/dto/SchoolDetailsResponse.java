package com.edunova.common.dto;

// ── Onboarding result ──────────────────────────────────────────
public record SchoolDetailsResponse(
        SchoolResponse      school,
        SubscriptionResponse subscription,
        AdminUserResponse   admin

) {}
