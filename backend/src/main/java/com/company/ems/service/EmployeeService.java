package com.company.ems.service;

import com.company.ems.dto.EmployeeDto;
import com.company.ems.dto.PagedResponse;
import com.company.ems.entity.Department;
import com.company.ems.entity.Employee;
import com.company.ems.entity.EmployeeStatus;
import com.company.ems.exception.DuplicateResourceException;
import com.company.ems.exception.ResourceNotFoundException;
import com.company.ems.mapper.EmployeeMapper;
import com.company.ems.repository.DepartmentRepository;
import com.company.ems.repository.EmployeeRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeDto> search(String keyword, Long departmentId, EmployeeStatus status,
                                              int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                StringUtils.hasText(sortBy) ? sortBy : "id");
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 10 : size, sort);

        Specification<Employee> spec = buildSpecification(keyword, departmentId, status);
        Page<Employee> result = employeeRepository.findAll(spec, pageable);

        List<EmployeeDto> content = result.getContent().stream().map(employeeMapper::toDto).toList();

        return PagedResponse.<EmployeeDto>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private Specification<Employee> buildSpecification(String keyword, Long departmentId, EmployeeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                Predicate byName = cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("employeeCode")), like),
                        cb.like(cb.lower(root.get("jobTitle")), like)
                );
                predicates.add(byName);
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public EmployeeDto findById(Long id) {
        return employeeMapper.toDto(getOrThrow(id));
    }

    public EmployeeDto create(EmployeeDto dto) {
        if (employeeRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An employee with email '" + dto.getEmail() + "' already exists");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));

        Employee employee = employeeMapper.toEntity(dto, department);
        employee.setEmployeeCode(generateNextEmployeeCode());
        employee.setStatus(EmployeeStatus.ACTIVE);

        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    public EmployeeDto update(Long id, EmployeeDto dto) {
        Employee employee = getOrThrow(id);

        if (!employee.getEmail().equalsIgnoreCase(dto.getEmail())
                && employeeRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("An employee with email '" + dto.getEmail() + "' already exists");
        }

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));

        employeeMapper.updateEntity(employee, dto, department);
        Employee saved = employeeRepository.save(employee);
        return employeeMapper.toDto(saved);
    }

    /**
     * Soft-delete: employees are deactivated rather than physically removed,
     * preserving history and satisfying the "Delete/deactivate" requirement
     * without breaking referential integrity or audit trails.
     */
    public void deactivate(Long id) {
        Employee employee = getOrThrow(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.save(employee);
    }

    private Employee getOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private String generateNextEmployeeCode() {
        Integer maxSuffix = employeeRepository.findMaxEmployeeCodeSuffix();
        int next = (maxSuffix == null ? 1000 : maxSuffix) + 1;
        String candidate = "EMP" + next;
        while (employeeRepository.existsByEmployeeCode(candidate)) {
            next++;
            candidate = "EMP" + next;
        }
        return candidate;
    }
}
