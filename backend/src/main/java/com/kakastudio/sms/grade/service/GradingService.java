package com.kakastudio.sms.grade.service;

import com.kakastudio.sms.grade.dto.GradeEntry;
import com.kakastudio.sms.section.dto.SectionDetail;
import com.kakastudio.sms.section.dto.SectionResponse;
import java.util.List;

/** A professor's own teaching: their sections, class lists and grades. */
public interface GradingService {

  /** The professor's sections in visible semesters, optionally one semester only. */
  List<SectionResponse> mySections(Long professorUserId, Long semesterId);

  /**
   * @throws com.kakastudio.sms.common.error.ForbiddenException for another professor's section
   */
  SectionDetail mySection(Long professorUserId, Long sectionId);

  /**
   * Records scores while the semester is running. Each score sets the letter and points from the
   * grade scale.
   */
  SectionDetail grade(Long professorUserId, Long sectionId, List<GradeEntry> grades);
}
