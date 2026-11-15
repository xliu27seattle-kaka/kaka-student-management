package com.kakastudio.sms.course.controller;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseSummary;
import com.kakastudio.sms.course.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The course catalogue, readable by every signed-in user. */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping
  public PageResponse<CourseSummary> list(
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return courseService.list(query, department, active, page, size);
  }

  @GetMapping("/{id}")
  public CourseDetail get(@PathVariable Long id) {
    return courseService.get(id);
  }
}
