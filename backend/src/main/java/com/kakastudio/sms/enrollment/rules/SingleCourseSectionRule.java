package com.kakastudio.sms.enrollment.rules;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** One section per course per semester (and not the same section twice). */
@Component
@Order(30)
public class SingleCourseSectionRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    Long courseId = context.section().getCourse().getId();
    Long semesterId = context.section().getSemester().getId();
    return context.history().stream()
        .filter(e -> e.isEnrolled())
        .filter(e -> e.getSection().getCourse().getId().equals(courseId))
        .filter(e -> e.getSection().getSemester().getId().equals(semesterId))
        .findFirst()
        .map(e ->
            e.getSection().getId().equals(context.section().getId())
                ? new Ineligibility(
                    IneligibilityReason.ALREADY_ENROLLED, "Already enrolled in this section")
                : new Ineligibility(
                    IneligibilityReason.ALREADY_ENROLLED,
                    "Already enrolled in section " + e.getSection().code()));
  }
}
