package com.kakastudio.sms.semester.controller;

import com.kakastudio.sms.semester.dto.SemesterResponse;
import com.kakastudio.sms.semester.service.SemesterService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Semesters students and professors can see (drafts are hidden). */
@RestController
@RequestMapping("/api/v1/semesters")
public class SemesterController {

  private final SemesterService semesterService;

  public SemesterController(SemesterService semesterService) {
    this.semesterService = semesterService;
  }

  @GetMapping
  public List<SemesterResponse> list() {
    return semesterService.listVisible();
  }
}
