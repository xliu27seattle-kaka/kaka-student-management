package com.kakastudio.sms.student.controller;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.student.dto.StudentCreateRequest;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.student.dto.StudentUpdateRequest;
import com.kakastudio.sms.student.service.StudentService;
import com.kakastudio.sms.user.domain.UserStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Student management for administrators. The path prefix restricts it to the ADMIN role. */
@RestController
@RequestMapping("/api/v1/admin/students")
public class AdminStudentController {

  private final StudentService studentService;

  public AdminStudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  @GetMapping
  public PageResponse<StudentResponse> list(
      @RequestParam(required = false) UserStatus status,
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return studentService.list(status, query, page, size);
  }

  @GetMapping("/{id}")
  public StudentResponse get(@PathVariable Long id) {
    return studentService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StudentResponse create(@Valid @RequestBody StudentCreateRequest request) {
    return studentService.create(request);
  }

  @PutMapping("/{id}")
  public StudentResponse update(
      @PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
    return studentService.update(id, request);
  }

  @PostMapping("/{id}/approve")
  public StudentResponse approve(@PathVariable Long id) {
    return studentService.approve(id);
  }

  @PostMapping("/{id}/reject")
  public StudentResponse reject(@PathVariable Long id) {
    return studentService.reject(id);
  }

  @PostMapping("/{id}/deactivate")
  public StudentResponse deactivate(@PathVariable Long id) {
    return studentService.deactivate(id);
  }

  @PostMapping("/{id}/reactivate")
  public StudentResponse reactivate(@PathVariable Long id) {
    return studentService.reactivate(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    studentService.delete(id);
  }
}
