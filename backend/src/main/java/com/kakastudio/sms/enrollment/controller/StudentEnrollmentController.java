package com.kakastudio.sms.enrollment.controller;

import com.kakastudio.sms.enrollment.dto.EnrollRequest;
import com.kakastudio.sms.enrollment.dto.EnrollmentResponse;
import com.kakastudio.sms.enrollment.dto.Offering;
import com.kakastudio.sms.enrollment.service.EnrollmentService;
import com.kakastudio.sms.student.service.CurrentStudent;
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

/** A student's own enrollment. The student is always the signed-in user. */
@RestController
@RequestMapping("/api/v1/student")
public class StudentEnrollmentController {

  private final EnrollmentService enrollmentService;
  private final CurrentStudent currentStudent;

  public StudentEnrollmentController(
      EnrollmentService enrollmentService, CurrentStudent currentStudent) {
    this.enrollmentService = enrollmentService;
    this.currentStudent = currentStudent;
  }

  @GetMapping("/semesters/{semesterId}/offerings")
  public List<Offering> offerings(@PathVariable Long semesterId) {
    return enrollmentService.offerings(currentStudent.id(), semesterId);
  }

  @GetMapping("/enrollments")
  public List<EnrollmentResponse> enrollments(@RequestParam(required = false) Long semesterId) {
    return enrollmentService.enrollments(currentStudent.id(), semesterId);
  }

  @PostMapping("/enrollments")
  @ResponseStatus(HttpStatus.CREATED)
  public EnrollmentResponse enroll(@Valid @RequestBody EnrollRequest request) {
    return enrollmentService.enroll(currentStudent.id(), request.sectionId());
  }

  @DeleteMapping("/enrollments/{id}")
  public EnrollmentResponse drop(@PathVariable Long id) {
    return enrollmentService.drop(currentStudent.id(), id);
  }
}
