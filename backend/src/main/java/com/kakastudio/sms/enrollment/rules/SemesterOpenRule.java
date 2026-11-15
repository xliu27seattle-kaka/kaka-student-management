package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.semester.domain.SemesterStatus;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class SemesterOpenRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    if (context.section().getSemester().getStatus() == SemesterStatus.READY) {
      return Optional.empty();
    }
    return Optional.of(
        new Ineligibility(IneligibilityReason.ENROLLMENT_CLOSED, "Enrollment is not open"));
  }
}
