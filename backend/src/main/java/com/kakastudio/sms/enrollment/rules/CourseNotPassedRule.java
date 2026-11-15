package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.grade.domain.GradeScale;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** A passed course cannot be taken again; a failed one can. */
@Component
@Order(40)
public class CourseNotPassedRule implements EnrollmentRule {

  private final GradeScale gradeScale;

  public CourseNotPassedRule(GradeScale gradeScale) {
    this.gradeScale = gradeScale;
  }

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    Long courseId = context.section().getCourse().getId();
    boolean passed =
        context.history().stream()
            .anyMatch(e ->
                e.getStatus() == EnrollmentStatus.COMPLETED
                    && e.getScore() != null
                    && gradeScale.isPassing(e.getScore())
                    && e.getSection().getCourse().getId().equals(courseId));
    return passed
        ? Optional.of(new Ineligibility(IneligibilityReason.ALREADY_COMPLETED, "Already completed"))
        : Optional.empty();
  }
}
