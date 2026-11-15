package com.kakastudio.sms.semester.service;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.dto.SemesterRequest;
import com.kakastudio.sms.semester.dto.SemesterResponse;
import java.util.List;

public interface SemesterService {

  /** Every semester, newest first: for administrators. */
  List<SemesterResponse> listAll();

  /** Semesters students and professors may see (not drafts), newest first. */
  List<SemesterResponse> listVisible();

  SemesterResponse get(Long id);

  /** A new semester always starts as {@code DRAFT}. */
  SemesterResponse create(SemesterRequest request);

  /** Name and dates can change until the semester ends; the code only while it is a draft. */
  SemesterResponse update(Long id, SemesterRequest request);

  /** Only a draft without sections. */
  void delete(Long id);

  /** Codes of the sections that still have an enrolled student without a score. */
  List<String> ungradedSections(Long id);

  /**
   * Moves the semester one step. Going back to draft needs nobody enrolled; ending needs every
   * enrolled student graded, and turns their enrollments into completed ones.
   */
  SemesterResponse transition(Long id, SemesterStatus target);
}
