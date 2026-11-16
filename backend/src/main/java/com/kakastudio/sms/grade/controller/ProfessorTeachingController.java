package com.kakastudio.sms.grade.controller;

import com.kakastudio.sms.grade.dto.GradesRequest;
import com.kakastudio.sms.grade.service.GradingService;
import com.kakastudio.sms.section.dto.CourseSectionDetail;
import com.kakastudio.sms.section.dto.CourseSectionResponse;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in professor's sections and grade entry. */
@RestController
@RequestMapping("/api/v1/professor/sections")
public class ProfessorTeachingController {

  private final GradingService gradingService;
  private final CurrentUserProvider currentUser;

  public ProfessorTeachingController(
      GradingService gradingService, CurrentUserProvider currentUser) {
    this.gradingService = gradingService;
    this.currentUser = currentUser;
  }

  @GetMapping
  public List<CourseSectionResponse> mySections(@RequestParam(required = false) Long semesterId) {
    return gradingService.mySections(currentUser.require().id(), semesterId);
  }

  @GetMapping("/{id}")
  public CourseSectionDetail mySection(@PathVariable Long id) {
    return gradingService.mySection(currentUser.require().id(), id);
  }

  @PutMapping("/{id}/grades")
  public CourseSectionDetail grade(
      @PathVariable Long id, @Valid @RequestBody GradesRequest request) {
    return gradingService.grade(currentUser.require().id(), id, request.grades());
  }
}
