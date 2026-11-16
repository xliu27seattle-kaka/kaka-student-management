package com.kakastudio.sms.section.service;

import com.kakastudio.sms.section.dto.CourseSectionDetail;
import com.kakastudio.sms.section.dto.CourseSectionRequest;
import com.kakastudio.sms.section.dto.CourseSectionResponse;
import java.util.List;

public interface CourseSectionService {

  List<CourseSectionResponse> listForSemester(Long semesterId);

  /** Every section of a course, newest semester first. */
  List<CourseSectionResponse> listForCourse(Long courseId);

  /** A section with its class list. */
  CourseSectionDetail get(Long id);

  /**
   * Adds a section to a draft semester. It gets the next free letter for its course and the
   * course's credits.
   */
  CourseSectionResponse create(Long semesterId, CourseSectionRequest request);

  /**
   * In a draft semester everything can change. Once enrollment is open only capacity (never below
   * the number enrolled) and room can. Nothing changes after the semester starts.
   */
  CourseSectionResponse update(Long id, CourseSectionRequest request);

  /** Only while the semester is a draft. */
  void delete(Long id);

  /** What a professor teaches in semesters that are open or running, for their profile page. */
  List<CourseSectionResponse> currentlyTaughtBy(Long professorId);
}
