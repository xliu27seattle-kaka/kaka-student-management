package com.kakastudio.sms.section.service;

import com.kakastudio.sms.section.dto.SectionDetail;
import com.kakastudio.sms.section.dto.SectionRequest;
import com.kakastudio.sms.section.dto.SectionResponse;
import java.util.List;

public interface SectionService {

  List<SectionResponse> listForSemester(Long semesterId);

  /** Every section of a course, newest semester first. */
  List<SectionResponse> listForCourse(Long courseId);

  /** A section with its class list. */
  SectionDetail get(Long id);

  /**
   * Adds a section to a draft semester. It gets the next free letter for its course and the
   * course's credits.
   */
  SectionResponse create(Long semesterId, SectionRequest request);

  /**
   * In a draft semester everything can change. Once enrollment is open only capacity (never below
   * the number enrolled) and room can. Nothing changes after the semester starts.
   */
  SectionResponse update(Long id, SectionRequest request);

  /** Only while the semester is a draft. */
  void delete(Long id);

  /** What a professor teaches in semesters that are open or running, for their profile page. */
  List<SectionResponse> currentlyTaughtBy(Long professorId);
}
