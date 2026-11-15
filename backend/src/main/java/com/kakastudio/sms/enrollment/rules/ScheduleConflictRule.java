package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.section.domain.MeetingTime;
import com.kakastudio.sms.section.domain.Section;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * No collision (see {@link MeetingTime#collidesWith}) with the student's other sections in the same
 * semester. The message says whether the classes overlap or are just too close together.
 */
@Component
@Order(70)
public class ScheduleConflictRule implements EnrollmentRule {

  @Override
  public Optional<Ineligibility> check(EnrollmentContext context) {
    Section target = context.section();
    for (Enrollment seat : context.otherActiveSeatsThisSemester()) {
      Section other = seat.getSection();
      Set<String> overlapDays = new LinkedHashSet<>();
      MeetingTime tooClose = null;
      MeetingTime tooCloseOther = null;
      for (MeetingTime mine : target.getMeetings()) {
        for (MeetingTime theirs : other.getMeetings()) {
          if (mine.overlaps(theirs)) {
            overlapDays.add(mine.dayLabel());
          } else if (mine.collidesWith(theirs) && tooClose == null) {
            tooClose = mine;
            tooCloseOther = theirs;
          }
        }
      }
      if (!overlapDays.isEmpty()) {
        return conflict("Overlaps " + other.code() + " on " + String.join("/", overlapDays));
      }
      if (tooClose != null) {
        String side = tooClose.getStart().isBefore(tooCloseOther.getStart()) ? "before" : "after";
        return conflict(
            "Needs " + MeetingTime.MIN_GAP_MINUTES + " minutes " + side + " " + other.code());
      }
    }
    return Optional.empty();
  }

  private static Optional<Ineligibility> conflict(String message) {
    return Optional.of(new Ineligibility(IneligibilityReason.SCHEDULE_CONFLICT, message));
  }
}
