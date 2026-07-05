package com.edunova.module.admin.student.dto;

import java.util.UUID;

public record GradeDTO(
        UUID id,
        String name,
        String displayName
) {
}
