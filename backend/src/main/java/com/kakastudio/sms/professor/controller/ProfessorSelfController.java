package com.kakastudio.sms.professor.controller;

import com.kakastudio.sms.professor.dto.ProfessorProfileRequest;
import com.kakastudio.sms.professor.dto.ProfessorProfileResponse;
import com.kakastudio.sms.professor.service.ProfessorProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A professor's own profile. The path prefix restricts it to the PROFESSOR role. */
@RestController
@RequestMapping("/api/v1/professor/profile")
public class ProfessorSelfController {

  private final ProfessorProfileService professorService;

  public ProfessorSelfController(ProfessorProfileService professorService) {
    this.professorService = professorService;
  }

  @GetMapping
  public ProfessorProfileResponse myProfile() {
    return professorService.myProfile();
  }

  @PutMapping
  public ProfessorProfileResponse updateMyProfile(
      @Valid @RequestBody ProfessorProfileRequest request) {
    return professorService.updateMyProfile(request);
  }
}
