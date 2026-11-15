package com.kakastudio.sms.enrollment.controller;

import com.kakastudio.sms.enrollment.dto.EnrollRequest;
import com.kakastudio.sms.enrollment.dto.EnrollmentResponse;
import com.kakastudio.sms.enrollment.dto.Offering;
import com.kakastudio.sms.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** An administrator enrolling or dropping a student on their behalf, under the same rules. */
@RestController
@RequestMapping("/api/v1/admin/students/{studentId}")
public class AdminEnrollmentController {

  private final EnrollmentService enrollmentService;

  public AdminEnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @GetMapping("/offerings")
  public List<Offering> offerings(@PathVariable Long studentId, @RequestParam Long semesterId) {
    return enrollmentService.offerings(studentId, semesterId);
  }

  @GetMapping("/enrollments")
  public List<EnrollmentResponse> enrollments(
      @PathVariable Long studentId, @RequestParam(required = false) Long semesterId) {
    return enrollmentService.enrollments(studentId, semesterId);
  }

  @PostMapping("/enrollments")
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse enroll(
      @PathVariable Long studentId, @Valid @RequestBody EnrollRequest request) {
    return enrollmentService.enroll(studentId, request.sectionId());
  }

  @DeleteMapping("/enrollments/{enrollmentId}")
  public EnrollmentResponse drop(@PathVariable Long studentId, @PathVariable Long enrollmentId) {
    return enrollmentService.drop(studentId, enrollmentId);
  }
}
