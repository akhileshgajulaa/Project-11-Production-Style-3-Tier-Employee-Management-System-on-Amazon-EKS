package com.company.ems.mapper;

import com.company.ems.dto.DepartmentDto;
import com.company.ems.entity.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentDto toDto(Department d, long employeeCount) {
        return DepartmentDto.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .employeeCount(employeeCount)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public Department toEntity(DepartmentDto dto) {
        return Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    public void updateEntity(Department entity, DepartmentDto dto) {
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
    }
}
