package com.edunova.common.dto;

// ── Onboarding result ──────────────────────────────────────────
public record OnboardSchoolResponse(
        SchoolResponse      school,
        SubscriptionResponse subscription,
        AdminUserResponse   adminUser,
        boolean             welcomeEmailSent,
        String              message
) {}
