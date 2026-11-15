package com.kakastudio.sms.enrollment.rules;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class SeatsAvailableRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    if (context.seatsTaken() < context.section().getCapacity()) {
      return Optional.empty();
    }
    return Optional.of(new Ineligibility(IneligibilityReason.SECTION_FULL, "Section full"));
  }
}
