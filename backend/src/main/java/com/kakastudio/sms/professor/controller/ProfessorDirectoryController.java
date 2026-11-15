package com.kakastudio.sms.professor.controller;

import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.service.ProfessorDirectoryService;
import com.kakastudio.sms.section.dto.SectionResponse;
import com.kakastudio.sms.section.service.SectionService;
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
  private final SectionService sectionService;

  public ProfessorDirectoryController(
      ProfessorDirectoryService professorService, SectionService sectionService) {
    this.professorService = professorService;
    this.sectionService = sectionService;
  }

  @GetMapping
  public List<ProfessorProfileResponse> directory() {
    return professorService.directory();
  }

  @GetMapping("/{id}")
  public ProfessorProfileResponse profile(@PathVariable Long id) {
    return professorService.profile(id);
  }

  /** What the professor teaches in semesters that are open or running. */
  @GetMapping("/{id}/sections")
  public List<SectionResponse> sections(@PathVariable Long id) {
    professorService.profile(id); // same visibility as the profile: active professors only
    return sectionService.currentlyTaughtBy(id);
  }
}
