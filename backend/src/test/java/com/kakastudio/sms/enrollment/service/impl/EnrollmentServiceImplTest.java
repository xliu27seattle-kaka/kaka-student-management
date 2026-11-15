package com.kakastudio.sms.enrollment.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.meeting;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.dto.EnrollmentResponse;
import com.kakastudio.sms.enrollment.dto.Offering;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.enrollment.repository.SectionSeatCount;
import com.kakastudio.sms.enrollment.rules.CourseInProgressRule;
import com.kakastudio.sms.enrollment.rules.CourseNotPassedRule;
import com.kakastudio.sms.enrollment.rules.PrerequisitesRule;
import com.kakastudio.sms.enrollment.rules.ScheduleConflictRule;
import com.kakastudio.sms.enrollment.rules.SeatsAvailableRule;
import com.kakastudio.sms.enrollment.rules.SemesterOpenRule;
import com.kakastudio.sms.enrollment.rules.SingleSectionRule;
import com.kakastudio.sms.enrollment.rules.StudentEligibleRule;
import com.kakastudio.sms.grade.domain.StandardGradeScale;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.mapper.SectionMapper;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.semester.service.SemesterLock;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class EnrollmentServiceImplTest {

  private final StudentRepository students = mock(StudentRepository.class);
  private final SectionRepository sections = mock(SectionRepository.class);
  private final SemesterRepository semesters = mock(SemesterRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final SemesterLock semesterLock = mock(SemesterLock.class);
  private EnrollmentServiceImpl service;

  private final Professor okafor = professor(1L, "Okafor");
  private final Semester open = semester(2L, "2027SP", SemesterStatus.READY);
  private final Course cs101 = course(1L, "CS101", 3);
  private final Course cs102 = course(2L, "CS102", 3);
  private final Course cs201 = course(3L, "CS201", 3, cs101);
  private final Student lena = student(1L);
  private final Section cs101a =
      section(10L, cs101, open, okafor, "A", meeting(MONDAY, "09:00", "10:00"));
  private final Section cs102a =
      section(11L, cs102, open, okafor, "A", meeting(TUESDAY, "09:00", "10:00"));
  private final Section cs201a =
      section(12L, cs201, open, okafor, "A", meeting(THURSDAY, "09:00", "10:00"));

  @BeforeEach
  void setUp() {
    StandardGradeScale scale = new StandardGradeScale();
    service =
        new EnrollmentServiceImpl(
            students,
            sections,
            semesters,
            enrollments,
            List.of(
                new SemesterOpenRule(),
                new StudentEligibleRule(),
                new SingleSectionRule(),
                new CourseNotPassedRule(scale),
                new CourseInProgressRule(),
                new PrerequisitesRule(scale),
                new SeatsAvailableRule(),
                new ScheduleConflictRule()),
            new SectionMapper(),
            semesterLock,
            Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC));
    when(students.findById(1L)).thenReturn(Optional.of(lena));
    when(students.findWithLockById(1L)).thenReturn(Optional.of(lena));
    when(semesters.findById(2L)).thenReturn(Optional.of(open));
    when(sections.findBySemesterId(2L)).thenReturn(List.of(cs101a, cs102a, cs201a));
    when(sections.findWithLockById(10L)).thenReturn(Optional.of(cs101a));
    when(sections.findWithLockById(11L)).thenReturn(Optional.of(cs102a));
    when(enrollments.findHistory(1L)).thenReturn(List.of());
    when(enrollments.countSeatsTaken(2L, EnrollmentStatus.ENROLLED))
        .thenReturn(List.of(seats(11L, 20)));
    when(enrollments.save(any())).thenAnswer(call -> call.getArgument(0));
  }

  private static SectionSeatCount seats(Long sectionId, long taken) {
    return new SectionSeatCount(sectionId, taken);
  }

  @Test
  void listsEverySectionWithWhyItIsUnavailable() {
    List<Offering> offerings = service.offerings(1L, 2L);

    assertThat(offerings)
        .extracting(o -> o.section().code())
        .containsExactly("CS101-A", "CS102-A", "CS201-A");
    assertThat(offerings.get(0).available()).isTrue();
    assertThat(offerings.get(1).available()).isFalse();
    assertThat(offerings.get(1).reasons()).extracting("message").containsExactly("Section full");
    assertThat(offerings.get(2).reasons()).extracting("message").containsExactly("Requires CS101");
  }

  @Test
  void aDraftSemesterIsNotVisible() {
    open.setStatus(SemesterStatus.DRAFT);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.offerings(1L, 2L));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_NOT_FOUND);
  }

  @Test
  void enrollLocksTheStudentBeforeTheSection() {
    when(enrollments.countBySectionIdAndStatus(10L, EnrollmentStatus.ENROLLED)).thenReturn(3L);

    EnrollmentResponse result = service.enroll(1L, 10L);

    assertThat(result.status()).isEqualTo(EnrollmentStatus.ENROLLED);
    InOrder order = inOrder(students, sections, semesterLock);
    order.verify(students).findWithLockById(1L);
    order.verify(sections).findWithLockById(10L);
    order.verify(semesterLock).forUse(open);
  }

  @Test
  void enrollRefusesWithTheFirstReason() {
    when(enrollments.countBySectionIdAndStatus(11L, EnrollmentStatus.ENROLLED)).thenReturn(20L);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.enroll(1L, 11L));

    assertThat(error.code()).isEqualTo(ErrorCode.NOT_ELIGIBLE);
    assertThat(error.getMessage()).isEqualTo("Section full");
  }

  @Test
  void enrollingAgainAfterADropReusesTheRow() {
    Enrollment dropped = enrollment(7L, lena, cs101a, EnrollmentStatus.DROPPED, null);
    when(enrollments.findHistory(1L)).thenReturn(List.of(dropped));
    when(enrollments.findByStudentIdAndSectionId(1L, 10L)).thenReturn(Optional.of(dropped));

    EnrollmentResponse result = service.enroll(1L, 10L);

    assertThat(result.id()).isEqualTo(7L);
    assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
  }

  @Test
  void droppingWhileOpenLeavesNoTrace() {
    Enrollment active = enrollment(7L, lena, cs101a, EnrollmentStatus.ENROLLED, null);
    when(enrollments.findById(7L)).thenReturn(Optional.of(active));

    service.drop(1L, 7L);

    assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    verify(semesterLock).forUse(open);
  }

  @Test
  void droppingAfterTheStartWithdrawsAndClearsTheScore() {
    open.setStatus(SemesterStatus.STARTED);
    Enrollment active = enrollment(7L, lena, cs101a, EnrollmentStatus.ENROLLED, 88);
    when(enrollments.findById(7L)).thenReturn(Optional.of(active));

    service.drop(1L, 7L);

    assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.WITHDRAWN);
    assertThat(active.getScore()).isNull();
  }

  @Test
  void nothingCanBeDroppedOnceTheSemesterEnded() {
    open.setStatus(SemesterStatus.ENDED);
    Enrollment done = enrollment(7L, lena, cs101a, EnrollmentStatus.ENROLLED, 88);
    when(enrollments.findById(7L)).thenReturn(Optional.of(done));

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.drop(1L, 7L));

    assertThat(error.code()).isEqualTo(ErrorCode.ENROLLMENT_LOCKED);
  }

  @Test
  void aStudentCannotDropSomeoneElsesEnrollment() {
    Enrollment other = enrollment(7L, student(2L), cs101a, EnrollmentStatus.ENROLLED, null);
    when(enrollments.findById(7L)).thenReturn(Optional.of(other));

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.drop(1L, 7L));

    assertThat(error.code()).isEqualTo(ErrorCode.ENROLLMENT_NOT_FOUND);
  }
}
