package com.edunova.module.attendance.mapper;


import com.edunova.module.attendance.dto.AttendanceDto;
import com.edunova.module.attendance.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceDto.Response toResponse(Attendance attendance) {
        return AttendanceDto.Response.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .name(attendance.getStudent().getFullName())
                //.admissionNo(attendance.getStudent().getAdmissionNo())
                .roll(attendance.getStudent().getAdmissionNo())
                .date(attendance.getDate())
                .markedAt(attendance.getMarkedAt())
                //.markedBy(attendance.getMarkedBy())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }
}