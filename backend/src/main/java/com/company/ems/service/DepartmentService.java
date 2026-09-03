package com.company.ems.service;

import com.company.ems.dto.DepartmentDto;
import com.company.ems.entity.Department;
import com.company.ems.exception.DuplicateResourceException;
import com.company.ems.exception.ResourceNotFoundException;
import com.company.ems.mapper.DepartmentMapper;
import com.company.ems.repository.DepartmentRepository;
import com.company.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    @Transactional(readOnly = true)
    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(d -> departmentMapper.toDto(d, d.getEmployees() != null ? countEmployees(d.getId()) : 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto findById(Long id) {
        Department department = getOrThrow(id);
        return departmentMapper.toDto(department, countEmployees(id));
    }

    public DepartmentDto create(DepartmentDto dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("A department named '" + dto.getName() + "' already exists");
        }
        Department saved = departmentRepository.save(departmentMapper.toEntity(dto));
        return departmentMapper.toDto(saved, 0);
    }

    public DepartmentDto update(Long id, DepartmentDto dto) {
        Department department = getOrThrow(id);

        if (!department.getName().equalsIgnoreCase(dto.getName())
                && departmentRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("A department named '" + dto.getName() + "' already exists");
        }

        departmentMapper.updateEntity(department, dto);
        Department saved = departmentRepository.save(department);
        return departmentMapper.toDto(saved, countEmployees(id));
    }

    public void delete(Long id) {
        Department department = getOrThrow(id);
        long employeeCount = countEmployees(id);
        if (employeeCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete department '" + department.getName() + "' because it still has "
                            + employeeCount + " employee(s) assigned. Reassign or deactivate them first.");
        }
        departmentRepository.delete(department);
    }

    private long countEmployees(Long departmentId) {
        return employeeRepository.count((root, query, cb) ->
                cb.equal(root.get("department").get("id"), departmentId));
    }

    private Department getOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
    }
}
