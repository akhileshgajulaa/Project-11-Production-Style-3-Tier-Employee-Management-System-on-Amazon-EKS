package com.company.ems.service;

import com.company.ems.dto.DashboardStatsDto;
import com.company.ems.entity.Employee;
import com.company.ems.entity.EmployeeStatus;
import com.company.ems.repository.DepartmentRepository;
import com.company.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public DashboardStatsDto getStatistics() {
        long total = employeeRepository.count();
        long active = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        long inactive = employeeRepository.countByStatus(EmployeeStatus.INACTIVE);
        long departments = departmentRepository.count();

        Map<String, Long> byDepartment = new LinkedHashMap<>();
        for (Object[] row : employeeRepository.countGroupedByDepartment()) {
            byDepartment.put((String) row[0], (Long) row[1]);
        }

        List<Employee> recent = employeeRepository.findRecentlyJoined(LocalDate.now().minusMonths(3));
        List<DashboardStatsDto.RecentEmployee> recentDtos = recent.stream()
                .limit(10)
                .map(e -> DashboardStatsDto.RecentEmployee.builder()
                        .id(e.getId())
                        .fullName(e.getFirstName() + " " + e.getLastName())
                        .department(e.getDepartment().getName())
                        .jobTitle(e.getJobTitle())
                        .joiningDate(e.getJoiningDate().format(DATE_FMT))
                        .build())
                .toList();

        return DashboardStatsDto.builder()
                .totalEmployees(total)
                .activeEmployees(active)
                .inactiveEmployees(inactive)
                .totalDepartments(departments)
                .employeesByDepartment(byDepartment)
                .recentlyJoined(recentDtos)
                .build();
    }
}
