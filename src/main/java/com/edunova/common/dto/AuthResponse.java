package com.edunova.common.dto;

// ── Auth ───────────────────────────────────────────────────────
public record AuthResponse(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
    public static String TOKEN_TYPE = "Bearer";
}
