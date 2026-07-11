package com.edunova.module.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AttendanceSectionDto {
    private UUID id;
    private String name;
    private UUID             sectionId;
    private String           sectionName;
    private String           roll;

    public AttendanceSectionDto(UUID id, String name, UUID sectionId, String sectionName, String roll) {
        this.id = id;
        this.name = name;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.roll = roll;
    }
}
