package com.kakastudio.sms.dashboard.controller;

import com.kakastudio.sms.dashboard.dto.DashboardResponse;
import com.kakastudio.sms.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {

  private final DashboardService dashboardService;

  public AdminDashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/api/v1/admin/dashboard")
  public DashboardResponse dashboard() {
    return dashboardService.adminSummary();
  }
}
