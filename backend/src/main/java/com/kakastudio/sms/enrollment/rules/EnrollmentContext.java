package com.kakastudio.sms.enrollment.rules;

import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.student.domain.Student;
import java.util.List;

/**
 * Everything the rules need to decide about one student and one section, loaded once.
 *
 * @param seatsTaken students currently enrolled in the section
 * @param history every enrollment of the student, in any semester and status
 */
public record EnrollmentContext(
    Student student, Section section, long seatsTaken, List<Enrollment> history) {

  /** The student's other active seats in the same semester as the section. */
  public List<Enrollment> otherActiveSeatsThisSemester() {
    return history.stream()
        .filter(Enrollment::isEnrolled)
        .filter(e -> e.getSection().getSemester().getId().equals(section.getSemester().getId()))
        .filter(e -> !e.getSection().getId().equals(section.getId()))
        .toList();
  }
}
