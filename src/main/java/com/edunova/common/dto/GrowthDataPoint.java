package com.edunova.common.dto;

public record GrowthDataPoint(
        String month,
        int    year,
        long   totalSchools,
        long   newSchools,
        long   totalStudents
) {}
