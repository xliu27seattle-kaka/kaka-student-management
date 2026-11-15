package com.kakastudio.sms.enrollment.rules;

import java.util.Optional;

/**
 * One condition a student must meet to enroll. The rules are Spring beans run in {@code @Order};
 * adding a condition means adding one class.
 */
public interface EnrollmentRule {

  /**
   * @return empty when the student meets the condition, otherwise why not
   */
  Optional<Ineligibility> check(EnrollmentContext context);
}
