package com.kakastudio.sms.graduation.controller;

import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.dto.ApplicationDto;
import com.kakastudio.sms.graduation.dto.GraduationStatusResponse;
import com.kakastudio.sms.graduation.service.GraduationService;
import com.kakastudio.sms.student.service.CurrentStudent;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GraduationController {

  private final GraduationService graduationService;
  private final CurrentStudent currentStudent;

  public GraduationController(GraduationService graduationService, CurrentStudent currentStudent) {
    this.graduationService = graduationService;
    this.currentStudent = currentStudent;
  }

  @GetMapping("/api/v1/student/graduation")
  public GraduationStatusResponse myStatus() {
    return graduationService.status(currentStudent.id());
  }

  @PostMapping("/api/v1/student/graduation/apply")
  public GraduationStatusResponse apply() {
    return graduationService.apply(currentStudent.id());
  }

  @GetMapping("/api/v1/admin/graduation-applications")
  public List<ApplicationDto> applications(
      @RequestParam(required = false) ApplicationStatus status) {
    return graduationService.applications(status);
  }

  @PostMapping("/api/v1/admin/graduation-applications/{id}/approve")
  public ApplicationDto approve(@PathVariable Long id) {
    return graduationService.approve(id);
  }

  @PostMapping("/api/v1/admin/graduation-applications/{id}/reject")
  public ApplicationDto reject(@PathVariable Long id) {
    return graduationService.reject(id);
  }
}
