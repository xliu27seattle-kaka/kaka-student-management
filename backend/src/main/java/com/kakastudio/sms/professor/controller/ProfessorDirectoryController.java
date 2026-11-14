package com.kakastudio.sms.professor.controller;

import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.service.ProfessorDirectoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Professor profiles, readable by every signed-in user and by nobody else. */
@RestController
@RequestMapping("/api/v1/professors")
public class ProfessorDirectoryController {

  private final ProfessorDirectoryService professorService;

  public ProfessorDirectoryController(ProfessorDirectoryService professorService) {
    this.professorService = professorService;
  }

  @GetMapping
  public List<ProfessorProfileResponse> directory() {
    return professorService.directory();
  }

  @GetMapping("/{id}")
  public ProfessorProfileResponse profile(@PathVariable Long id) {
    return professorService.profile(id);
  }
}
