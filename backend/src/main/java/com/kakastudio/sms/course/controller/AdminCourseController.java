package com.kakastudio.sms.course.controller;

import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseRequest;
import com.kakastudio.sms.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Catalogue changes, for administrators only. */
@RestController
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController {

  private final CourseService courseService;

  public AdminCourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CourseDetail create(@Valid @RequestBody CourseRequest request) {
    return courseService.create(request);
  }

  @PutMapping("/{id}")
  public CourseDetail update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
    return courseService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    courseService.delete(id);
  }
}
