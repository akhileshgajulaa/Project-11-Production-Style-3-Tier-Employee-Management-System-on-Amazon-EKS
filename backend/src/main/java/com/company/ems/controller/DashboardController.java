package com.company.ems.controller;

import com.company.ems.dto.DashboardStatsDto;
import com.company.ems.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregate HR statistics for the dashboard view")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics: totals, breakdown by department, recent joiners")
    public ResponseEntity<DashboardStatsDto> statistics() {
        return ResponseEntity.ok(dashboardService.getStatistics());
    }
}
