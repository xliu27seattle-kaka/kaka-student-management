package com.kakastudio.sms.semester.controller;

import com.kakastudio.sms.semester.dto.SemesterRequest;
import com.kakastudio.sms.semester.dto.SemesterResponse;
import com.kakastudio.sms.semester.dto.SemesterTransitionRequest;
import com.kakastudio.sms.semester.service.SemesterService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/semesters")
public class AdminSemesterController {

  private final SemesterService semesterService;

  public AdminSemesterController(SemesterService semesterService) {
    this.semesterService = semesterService;
  }

  @GetMapping
  public List<SemesterResponse> list() {
    return semesterService.listAll();
  }

  @GetMapping("/{id}")
  public SemesterResponse get(@PathVariable Long id) {
    return semesterService.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SemesterResponse create(@Valid @RequestBody SemesterRequest request) {
    return semesterService.create(request);
  }

  @PutMapping("/{id}")
  public SemesterResponse update(
      @PathVariable Long id, @Valid @RequestBody SemesterRequest request) {
    return semesterService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    semesterService.delete(id);
  }

  /** Asked before ending a semester, so the confirmation can name what is missing. */
  @GetMapping("/{id}/ungraded-sections")
  public List<String> ungradedSections(@PathVariable Long id) {
    return semesterService.ungradedSections(id);
  }

  @PostMapping("/{id}/transition")
  public SemesterResponse transition(
      @PathVariable Long id, @Valid @RequestBody SemesterTransitionRequest request) {
    return semesterService.transition(id, request.target());
  }
}
