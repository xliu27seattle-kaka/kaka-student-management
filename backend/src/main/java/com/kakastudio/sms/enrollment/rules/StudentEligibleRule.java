package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class StudentEligibleRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    if (context.student().getAcademicStatus() == AcademicStatus.GRADUATED) {
      return Optional.of(
          new Ineligibility(IneligibilityReason.ACCOUNT_NOT_ELIGIBLE, "Already graduated"));
    }
    if (context.student().getUser().getStatus() != UserStatus.ACTIVE) {
      return Optional.of(
          new Ineligibility(IneligibilityReason.ACCOUNT_NOT_ELIGIBLE, "Account not eligible"));
    }
    return Optional.empty();
  }
}
