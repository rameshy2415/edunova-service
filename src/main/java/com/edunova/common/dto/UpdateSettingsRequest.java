package com.edunova.common.dto;

// ── Update SA settings ─────────────────────────────────────────
public record UpdateSettingsRequest(
        String key,
        String value
) {}
