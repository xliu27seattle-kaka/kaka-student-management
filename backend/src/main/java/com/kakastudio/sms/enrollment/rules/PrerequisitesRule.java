package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.grade.domain.GradeScale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Every prerequisite passed in a semester that has ended. A course still running does not count
 * yet, which is why a semester is ended before the next one starts.
 */
@Component
@Order(50)
public class PrerequisitesRule implements EnrollmentRule {

  private final GradeScale gradeScale;

  public PrerequisitesRule(GradeScale gradeScale) {
    this.gradeScale = gradeScale;
  }

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    Set<Long> passed =
        context.history().stream()
            .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
            .filter(e -> e.getScore() != null && gradeScale.isPassing(e.getScore()))
            .map(e -> e.getSection().getCourse().getId())
            .collect(Collectors.toSet());
    List<String> missing =
        context.section().getCourse().getPrerequisites().stream()
            .filter(prerequisite -> !passed.contains(prerequisite.getId()))
            .map(Course::getCode)
            .sorted()
            .toList();
    if (missing.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        new Ineligibility(
            IneligibilityReason.MISSING_PREREQUISITE, "Requires " + String.join(", ", missing)));
  }
}
