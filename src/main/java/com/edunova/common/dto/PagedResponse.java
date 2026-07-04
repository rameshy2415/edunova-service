package com.edunova.common.dto;

// ── Generic wrappers ───────────────────────────────────────────
public record PagedResponse<T>(
        java.util.List<T> content,
        int  page,
        int  size,
        long totalElements,
        int  totalPages,
        boolean last
) {}
