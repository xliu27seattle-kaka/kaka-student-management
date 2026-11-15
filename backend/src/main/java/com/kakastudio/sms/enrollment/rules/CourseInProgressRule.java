package com.kakastudio.sms.enrollment.rules;

import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A course the student is taking right now in another semester cannot be taken again before that
 * attempt has a final grade: if it is passed, the second seat would be a retake of a passed course.
 */
@Component
@Order(45)
public class CourseInProgressRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    Long courseId = context.section().getCourse().getId();
    Long semesterId = context.section().getSemester().getId();
    return context.history().stream()
        .filter(e -> e.isEnrolled())
        .filter(e -> e.getSection().getCourse().getId().equals(courseId))
        .filter(e -> !e.getSection().getSemester().getId().equals(semesterId))
        .findFirst()
        .map(e ->
            new Ineligibility(
                IneligibilityReason.COURSE_IN_PROGRESS,
                "Already taking "
                    + e.getSection().getCourse().getCode()
                    + " in "
                    + e.getSection().getSemester().getName()));
  }
}
