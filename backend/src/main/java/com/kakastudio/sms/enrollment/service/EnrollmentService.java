package com.kakastudio.sms.enrollment.service;

import com.kakastudio.sms.enrollment.dto.EnrollmentResponse;
import com.kakastudio.sms.enrollment.dto.Offering;
import java.util.List;

/**
 * Enrolling and dropping. The same rules apply whether the student acts or an administrator acts on
 * their behalf; there is no override.
 */
public interface EnrollmentService {

  /** Every section of a visible semester, each marked available or not, with the reasons. */
  List<Offering> offerings(Long studentId, Long semesterId);

  /** The student's enrollments (dropped ones left out), optionally for one semester. */
  List<EnrollmentResponse> enrollments(Long studentId, Long semesterId);

  /**
   * Re-runs every rule inside one transaction, holding a lock on the student and then on the
   * section.
   *
   * @throws com.kakastudio.sms.common.error.RuleViolationException with the first reason
   */
  EnrollmentResponse enroll(Long studentId, Long sectionId);

  /** Dropped while enrollment is open, withdrawn (W) after the start, refused after the end. */
  EnrollmentResponse drop(Long studentId, Long enrollmentId);
}
