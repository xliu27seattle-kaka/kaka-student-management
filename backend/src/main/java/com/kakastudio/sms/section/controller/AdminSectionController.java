package com.kakastudio.sms.section.controller;

import com.kakastudio.sms.section.dto.SectionDetail;
import com.kakastudio.sms.section.dto.SectionRequest;
import com.kakastudio.sms.section.dto.SectionResponse;
import com.kakastudio.sms.section.service.SectionService;
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
@RequestMapping("/api/v1/admin")
public class AdminSectionController {

  private final SectionService sectionService;

  public AdminSectionController(SectionService sectionService) {
    this.sectionService = sectionService;
  }

  @GetMapping("/semesters/{semesterId}/sections")
  public List<SectionResponse> list(@PathVariable Long semesterId) {
    return sectionService.listForSemester(semesterId);
  }

  @GetMapping("/courses/{courseId}/sections")
  public List<SectionResponse> listForCourse(@PathVariable Long courseId) {
    return sectionService.listForCourse(courseId);
  }

  @PostMapping("/semesters/{semesterId}/sections")
  @ResponseStatus(HttpStatus.CREATED)
  public SectionResponse create(
      @PathVariable Long semesterId, @Valid @RequestBody SectionRequest request) {
    return sectionService.create(semesterId, request);
  }

  @GetMapping("/sections/{id}")
  public SectionDetail get(@PathVariable Long id) {
    return sectionService.get(id);
  }

  @PutMapping("/sections/{id}")
  public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
    return sectionService.update(id, request);
  }

  @DeleteMapping("/sections/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    sectionService.delete(id);
  }
}
