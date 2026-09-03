package com.company.ems.mapper;

import com.company.ems.dto.EmployeeDto;
import com.company.ems.entity.Department;
import com.company.ems.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public EmployeeDto toDto(Employee e) {
        return EmployeeDto.builder()
                .id(e.getId())
                .employeeCode(e.getEmployeeCode())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .dateOfBirth(e.getDateOfBirth())
                .gender(e.getGender())
                .jobTitle(e.getJobTitle())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .joiningDate(e.getJoiningDate())
                .salary(e.getSalary())
                .status(e.getStatus())
                .address(e.getAddress())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public Employee toEntity(EmployeeDto dto, Department department) {
        return Employee.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .jobTitle(dto.getJobTitle())
                .department(department)
                .joiningDate(dto.getJoiningDate())
                .salary(dto.getSalary())
                .address(dto.getAddress())
                .build();
    }

    public void updateEntity(Employee entity, EmployeeDto dto, Department department) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setGender(dto.getGender());
        entity.setJobTitle(dto.getJobTitle());
        entity.setDepartment(department);
        entity.setJoiningDate(dto.getJoiningDate());
        entity.setSalary(dto.getSalary());
        entity.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }
}
