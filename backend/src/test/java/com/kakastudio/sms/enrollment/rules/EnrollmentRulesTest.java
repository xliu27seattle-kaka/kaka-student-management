package com.kakastudio.sms.enrollment.rules;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.graded;
import static com.kakastudio.sms.testsupport.Catalog.meeting;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.grade.domain.StandardGradeScale;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnrollmentRulesTest {

  private final StandardGradeScale scale = new StandardGradeScale();
  private final Professor okafor = professor(1L, "Okafor");
  private final Semester past = semester(1L, "2026SP", SemesterStatus.ENDED);
  private final Semester open = semester(2L, "2027SP", SemesterStatus.READY);
  private final Course cs101 = course(1L, "CS101", 3);
  private final Course math120 = course(2L, "MATH120", 3);
  private final Course cs201 = course(3L, "CS201", 3, cs101, math120);
  private final Course cs240 = course(4L, "CS240", 3);
  private final Student lena = student(1L);

  private final Section cs201a =
      section(
          10L,
          cs201,
          open,
          okafor,
          "A",
          meeting(MONDAY, "10:00", "11:15"),
          meeting(WEDNESDAY, "10:00", "11:15"));

  private EnrollmentContext context(Section target, long seatsTaken, Enrollment... history) {
    return new EnrollmentContext(lena, target, seatsTaken, List.of(history));
  }

  private static Optional<String> message(Optional<Ineligibility> result) {
    return result.map(Ineligibility::message);
  }

  @Test
  void semesterMustBeOpen() {
    open.setStatus(SemesterStatus.STARTED);
    assertThat(new SemesterOpenRule().check(context(cs201a, 0)))
        .map(Ineligibility::reason)
        .contains(IneligibilityReason.ENROLLMENT_CLOSED);
    open.setStatus(SemesterStatus.READY);
    assertThat(new SemesterOpenRule().check(context(cs201a, 0))).isEmpty();
  }

  @Test
  void studentMustBeActiveAndNotGraduated() {
    assertThat(new StudentEligibleRule().check(context(cs201a, 0))).isEmpty();
    lena.setAcademicStatus(AcademicStatus.GRADUATED);
    assertThat(message(new StudentEligibleRule().check(context(cs201a, 0))))
        .contains("Already graduated");
    lena.setAcademicStatus(AcademicStatus.ENROLLED);
    lena.getUser().setStatus(UserStatus.INACTIVE);
    assertThat(message(new StudentEligibleRule().check(context(cs201a, 0))))
        .contains("Account not eligible");
  }

  @Test
  void oneSectionPerCoursePerSemester() {
    Section cs201b = section(11L, cs201, open, okafor, "B", meeting(TUESDAY, "10:00", "11:15"));
    Enrollment inB = enrollment(1L, lena, cs201b, EnrollmentStatus.ENROLLED, null);
    assertThat(message(new SingleSectionRule().check(context(cs201a, 0, inB))))
        .contains("Already enrolled in section CS201-B");

    Enrollment dropped = enrollment(2L, lena, cs201b, EnrollmentStatus.DROPPED, null);
    assertThat(new SingleSectionRule().check(context(cs201a, 0, dropped))).isEmpty();
  }

  @Test
  void aPassedCourseCannotBeTakenAgainButAFailedOneCan() {
    Section old = section(20L, cs201, past, okafor, "A");
    assertThat(
            message(
                new CourseNotPassedRule(scale)
                    .check(context(cs201a, 0, graded(1L, lena, old, 85, "B", "3.0")))))
        .contains("Already completed");
    assertThat(
            new CourseNotPassedRule(scale)
                .check(context(cs201a, 0, graded(1L, lena, old, 40, "F", "0.0"))))
        .isEmpty();
  }

  @Test
  void everyPrerequisiteMustBePassedInAnEndedSemester() {
    Section cs101old = section(21L, cs101, past, okafor, "A");
    Section mathOld = section(22L, math120, past, okafor, "A");
    PrerequisitesRule rule = new PrerequisitesRule(scale);

    assertThat(message(rule.check(context(cs201a, 0)))).contains("Requires CS101, MATH120");
    assertThat(message(rule.check(context(cs201a, 0, graded(1L, lena, cs101old, 70, "C-", "1.7")))))
        .contains("Requires MATH120");
    assertThat(message(rule.check(context(cs201a, 0, graded(1L, lena, cs101old, 59, "F", "0.0")))))
        .contains("Requires CS101, MATH120");
    assertThat(
            rule.check(
                context(
                    cs201a,
                    0,
                    graded(1L, lena, cs101old, 70, "C-", "1.7"),
                    graded(2L, lena, mathOld, 95, "A", "4.0"))))
        .isEmpty();
  }

  @Test
  void aSectionCanBeFull() {
    assertThat(message(new SeatsAvailableRule().check(context(cs201a, 20))))
        .contains("Section full");
    assertThat(new SeatsAvailableRule().check(context(cs201a, 19))).isEmpty();
  }

  @Test
  void overlappingClassesAreNamedWithTheirDays() {
    Section cs240a =
        section(
            30L,
            cs240,
            open,
            okafor,
            "A",
            meeting(MONDAY, "11:00", "12:00"),
            meeting(WEDNESDAY, "11:00", "12:00"));
    Enrollment in240 = enrollment(1L, lena, cs240a, EnrollmentStatus.ENROLLED, null);

    assertThat(message(new ScheduleConflictRule().check(context(cs201a, 0, in240))))
        .contains("Overlaps CS240-A on Mon/Wed");
  }

  @Test
  void classesTooCloseTogetherAskForTenMinutes() {
    Section cs240a = section(30L, cs240, open, okafor, "A", meeting(MONDAY, "11:20", "12:00"));
    Enrollment in240 = enrollment(1L, lena, cs240a, EnrollmentStatus.ENROLLED, null);

    assertThat(message(new ScheduleConflictRule().check(context(cs201a, 0, in240))))
        .contains("Needs 10 minutes before CS240-A");
  }

  @Test
  void classesInOtherSemestersOrDroppedDoNotConflict() {
    Section pastClash = section(31L, cs240, past, okafor, "A", meeting(MONDAY, "10:00", "11:00"));
    Section droppedClash =
        section(32L, cs240, open, okafor, "B", meeting(MONDAY, "10:00", "11:00"));

    assertThat(
            new ScheduleConflictRule()
                .check(
                    context(
                        cs201a,
                        0,
                        enrollment(1L, lena, pastClash, EnrollmentStatus.COMPLETED, 90),
                        enrollment(2L, lena, droppedClash, EnrollmentStatus.DROPPED, null))))
        .isEmpty();
  }

  @Test
  void aCourseTakenNowInAnotherSemesterCannotBeTakenAgain() {
    Semester running = semester(3L, "2026FA", SemesterStatus.STARTED);
    running.setName("Fall 2026");
    Section now = section(40L, cs201, running, okafor, "B");
    Enrollment inProgress = enrollment(1L, lena, now, EnrollmentStatus.ENROLLED, null);

    assertThat(message(new CourseInProgressRule().check(context(cs201a, 0, inProgress))))
        .contains("Already taking CS201 in Fall 2026");
    assertThat(new CourseInProgressRule().check(context(cs201a, 0))).isEmpty();
  }

  @Test
  void springRunsTheRulesInTheDocumentedOrder() {
    List<EnrollmentRule> rules =
        new java.util.ArrayList<>(
            List.of(
                new ScheduleConflictRule(),
                new SeatsAvailableRule(),
                new PrerequisitesRule(scale),
                new CourseInProgressRule(),
                new CourseNotPassedRule(scale),
                new SingleSectionRule(),
                new StudentEligibleRule(),
                new SemesterOpenRule()));
    org.springframework.core.annotation.AnnotationAwareOrderComparator.sort(rules);

    assertThat(rules)
        .extracting(rule -> rule.getClass().getSimpleName())
        .containsExactly(
            "SemesterOpenRule",
            "StudentEligibleRule",
            "SingleSectionRule",
            "CourseNotPassedRule",
            "CourseInProgressRule",
            "PrerequisitesRule",
            "SeatsAvailableRule",
            "ScheduleConflictRule");
  }
}
