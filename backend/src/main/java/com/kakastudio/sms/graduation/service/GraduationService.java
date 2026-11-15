package com.kakastudio.sms.graduation.service;

import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.dto.ApplicationDto;
import com.kakastudio.sms.graduation.dto.GraduationStatusResponse;
import java.util.List;

/**
 * Graduation needs {@value
 * com.kakastudio.sms.graduation.service.impl.GraduationServiceImpl#REQUIRED_CREDITS} earned
 * credits, no running semester left unfinished, and an administrator's approval.
 */
public interface GraduationService {

  GraduationStatusResponse status(Long studentId);

  /**
   * @throws com.kakastudio.sms.common.error.RuleViolationException with the first reason the
   *     student cannot apply
   */
  GraduationStatusResponse apply(Long studentId);

  /** For administrators; all applications when {@code status} is {@code null}. */
  List<ApplicationDto> applications(ApplicationStatus status);

  /**
   * Graduates the student: certificate number, official transcript, enrollments in open semesters
   * dropped, an email.
   */
  ApplicationDto approve(Long applicationId);

  ApplicationDto reject(Long applicationId);
}
