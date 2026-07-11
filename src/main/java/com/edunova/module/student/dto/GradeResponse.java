package com.edunova.module.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GradeResponse {

    private UUID id;
    private String name;
    private String displayName;
    private Integer order;
    private List<SectionResponse> sections;
}
