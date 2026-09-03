package com.company.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalEmployees;
    private long activeEmployees;
    private long inactiveEmployees;
    private long totalDepartments;
    private Map<String, Long> employeesByDepartment;
    private List<RecentEmployee> recentlyJoined;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecentEmployee {
        private Long id;
        private String fullName;
        private String department;
        private String jobTitle;
        private String joiningDate;
    }
}
