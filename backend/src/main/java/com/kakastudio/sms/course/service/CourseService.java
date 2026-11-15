package com.kakastudio.sms.course.service;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.course.dto.CourseDetail;
import com.kakastudio.sms.course.dto.CourseRequest;
import com.kakastudio.sms.course.dto.CourseSummary;

public interface CourseService {

  /** The catalogue by code; every filter is optional. */
  PageResponse<CourseSummary> list(
      String query, String department, Boolean active, int page, int size);

  CourseDetail get(Long id);

  /**
   * @throws com.kakastudio.sms.common.error.ConflictException if the code is taken
   */
  CourseDetail create(CourseRequest request);

  /**
   * Changing prerequisites affects future enrollments only.
   *
   * @throws com.kakastudio.sms.common.error.RuleViolationException if the change creates a cycle
   */
  CourseDetail update(Long id, CourseRequest request);

  /** Only a course nothing depends on: no sections and not a prerequisite of another course. */
  void delete(Long id);
}
