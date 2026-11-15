package com.kakastudio.sms.section.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.meeting;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.course.repository.CourseRepository;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.dto.MeetingRequest;
import com.kakastudio.sms.section.dto.SectionRequest;
import com.kakastudio.sms.section.dto.SectionResponse;
import com.kakastudio.sms.section.mapper.SectionMapper;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.user.domain.UserStatus;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SectionServiceImplTest {

  private final SectionRepository sections = mock(SectionRepository.class);
  private final SemesterRepository semesters = mock(SemesterRepository.class);
  private final CourseRepository courses = mock(CourseRepository.class);
  private final ProfessorRepository professors = mock(ProfessorRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private SectionServiceImpl service;

  private Course cs101;
  private Course cs201;
  private Professor okafor;
  private Semester draft;

  @BeforeEach
  void setUp() {
    cs101 = course(1L, "CS101", 3);
    cs201 = course(2L, "CS201", 4);
    okafor = professor(1L, "Okafor");
    draft = semester(5L, "2027SP", SemesterStatus.DRAFT);
    when(semesters.findById(5L)).thenReturn(Optional.of(draft));
    when(courses.findById(1L)).thenReturn(Optional.of(cs101));
    when(courses.findById(2L)).thenReturn(Optional.of(cs201));
    when(professors.findById(1L)).thenReturn(Optional.of(okafor));
    when(sections.save(any())).thenAnswer(call -> call.getArgument(0));
    when(sections.findLetters(any(), any())).thenReturn(List.of());
    when(sections.findByProfessorIdAndSemesterId(any(), any())).thenReturn(List.of());
    service =
        new SectionServiceImpl(
            sections, semesters, courses, professors, enrollments, new SectionMapper());
  }

  private static MeetingRequest slot(DayOfWeek day, String start, String end) {
    return new MeetingRequest(day, LocalTime.parse(start), LocalTime.parse(end));
  }

  private static SectionRequest request(Long courseId, Integer capacity, MeetingRequest... slots) {
    return new SectionRequest(courseId, 1L, capacity, "Room 2", List.of(slots));
  }

  @Test
  void createsTheNextLetterWithTheCourseCredits() {
    when(sections.findLetters(2L, 5L)).thenReturn(List.of("A"));

    SectionResponse created = service.create(5L, request(2L, null, slot(MONDAY, "10:00", "11:15")));

    assertThat(created.letter()).isEqualTo("B");
    assertThat(created.code()).isEqualTo("CS201-B");
    assertThat(created.credits()).isEqualTo(4);
    assertThat(created.capacity()).isEqualTo(20);
  }

  @Test
  void sectionsAreAddedOnlyToADraftSemester() {
    draft.setStatus(SemesterStatus.READY);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.create(5L, request(1L, 20, slot(MONDAY, "10:00", "11:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.SECTION_NOT_EDITABLE);
  }

  @Test
  void refusesARetiredCourse() {
    cs101.setActive(false);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.create(5L, request(1L, 20, slot(MONDAY, "10:00", "11:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.COURSE_INACTIVE);
  }

  @Test
  void refusesAnInactiveProfessor() {
    okafor.getUser().setStatus(UserStatus.INACTIVE);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.create(5L, request(1L, 20, slot(MONDAY, "10:00", "11:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.PROFESSOR_INACTIVE);
  }

  @Test
  void refusesAMeetingThatEndsBeforeItStarts() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.create(5L, request(1L, 20, slot(MONDAY, "11:00", "10:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.MEETING_INVALID);
  }

  @Test
  void refusesMeetingsOfOneSectionThatCollide() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.create(
                    5L,
                    request(
                        1L, 20, slot(MONDAY, "10:00", "11:00"), slot(MONDAY, "11:05", "12:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.MEETING_OVERLAP);
  }

  @Test
  void refusesAProfessorScheduleClashAndNamesTheSection() {
    Section existing = section(9L, cs201, draft, okafor, "A", meeting(WEDNESDAY, "13:00", "14:15"));
    when(sections.findByProfessorIdAndSemesterId(1L, 5L)).thenReturn(List.of(existing));

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.create(5L, request(1L, 20, slot(WEDNESDAY, "14:20", "15:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.PROFESSOR_SCHEDULE_CONFLICT);
    assertThat(error.getMessage()).contains("CS201-A", "Wed 13:00–14:15");
  }

  @Test
  void tenMinutesAfterAnotherClassIsFineForTheProfessor() {
    Section existing = section(9L, cs201, draft, okafor, "A", meeting(WEDNESDAY, "13:00", "14:15"));
    when(sections.findByProfessorIdAndSemesterId(1L, 5L)).thenReturn(List.of(existing));

    SectionResponse created =
        service.create(5L, request(1L, 20, slot(WEDNESDAY, "14:25", "15:00")));

    assertThat(created.meetings()).hasSize(1);
  }

  @Test
  void onceEnrollmentIsOpenOnlyCapacityAndRoomChange() {
    Section existing = section(9L, cs101, draft, okafor, "A", meeting(MONDAY, "10:00", "11:00"));
    draft.setStatus(SemesterStatus.READY);
    when(sections.findWithLockById(9L)).thenReturn(Optional.of(existing));
    when(enrollments.countBySectionIdAndStatus(9L, EnrollmentStatus.ENROLLED)).thenReturn(4L);

    SectionResponse updated = service.update(9L, request(1L, 30, slot(MONDAY, "10:00", "11:00")));
    assertThat(updated.capacity()).isEqualTo(30);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.update(9L, request(1L, 30, slot(MONDAY, "12:00", "13:00"))));
    assertThat(error.code()).isEqualTo(ErrorCode.SECTION_NOT_EDITABLE);
  }

  @Test
  void capacityCannotDropBelowTheNumberEnrolled() {
    Section existing = section(9L, cs101, draft, okafor, "A", meeting(MONDAY, "10:00", "11:00"));
    draft.setStatus(SemesterStatus.READY);
    when(sections.findWithLockById(9L)).thenReturn(Optional.of(existing));
    when(enrollments.countBySectionIdAndStatus(9L, EnrollmentStatus.ENROLLED)).thenReturn(12L);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.update(9L, request(1L, 10, slot(MONDAY, "10:00", "11:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.CAPACITY_BELOW_ENROLLED);
  }

  @Test
  void nothingChangesOnceTheSemesterStarted() {
    Section existing = section(9L, cs101, draft, okafor, "A", meeting(MONDAY, "10:00", "11:00"));
    draft.setStatus(SemesterStatus.STARTED);
    when(sections.findWithLockById(9L)).thenReturn(Optional.of(existing));

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.update(9L, request(1L, 25, slot(MONDAY, "10:00", "11:00"))));

    assertThat(error.code()).isEqualTo(ErrorCode.SECTION_NOT_EDITABLE);
  }
}
