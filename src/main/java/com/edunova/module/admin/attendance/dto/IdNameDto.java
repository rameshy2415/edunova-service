package com.edunova.module.admin.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class IdNameDto {
    private UUID id;
    private String name;
}
