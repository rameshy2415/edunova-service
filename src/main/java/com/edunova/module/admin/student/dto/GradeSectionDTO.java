package com.edunova.module.admin.student.dto;

import java.util.UUID;

public record GradeSectionDTO(
        UUID gradeId,
        String gradeName,
        String gradeDisplayName,
        Integer order,
        UUID sectionId,
        String sectionName,
        String sectionDisplayName
) {}
