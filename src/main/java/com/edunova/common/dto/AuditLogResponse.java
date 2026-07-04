package com.edunova.common.dto;

import java.time.Instant;
import java.util.UUID;

// ── Audit log ──────────────────────────────────────────────────
public record AuditLogResponse(
        UUID id,
        String  superAdminEmail,
        String  action,
        String  targetType,
        UUID    targetId,
        String  targetName,
        String  oldValues,
        String  newValues,
        String  ipAddress,
        Instant createdAt
) {}
