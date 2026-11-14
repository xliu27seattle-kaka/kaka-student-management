package com.kakastudio.sms.professor.controller;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.professor.dto.ProfessorCreateRequest;
import com.kakastudio.sms.professor.dto.ProfessorResponse;
import com.kakastudio.sms.professor.dto.ProfessorUpdateRequest;
import com.kakastudio.sms.professor.service.ProfessorAdminService;
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

/** Professor management for administrators. */
@RestController
@RequestMapping("/api/v1/admin/professors")
public class AdminProfessorController {

  private final ProfessorAdminService professorService;

  public AdminProfessorController(ProfessorAdminService professorService) {
    this.professorService = professorService;
  }

  @GetMapping
  public PageResponse<ProfessorResponse> list(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return professorService.list(query, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProfessorResponse create(@Valid @RequestBody ProfessorCreateRequest request) {
    return professorService.create(request);
  }

  @PutMapping("/{id}")
  public ProfessorResponse update(
      @PathVariable Long id, @Valid @RequestBody ProfessorUpdateRequest request) {
    return professorService.update(id, request);
  }

  @PostMapping("/{id}/deactivate")
  public ProfessorResponse deactivate(@PathVariable Long id) {
    return professorService.deactivate(id);
  }

  @PostMapping("/{id}/reactivate")
  public ProfessorResponse reactivate(@PathVariable Long id) {
    return professorService.reactivate(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    professorService.delete(id);
  }
}
