package com.edunova.common.dto;

import java.time.Instant;
import java.util.UUID;

// ── School ─────────────────────────────────────────────────────
public record SchoolResponse(
        UUID id,
        String  name,
        String  board,
        String  city,
        String  state,
        String  pincode,
        String  phone,
        String  email,
        String  website,
        String  principalName,
        Integer establishedYear,
        String  affiliationNo,
        String  address,
        Boolean isActive,
        Instant createdAt,
        int students,
        int teachers
) {}
