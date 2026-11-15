package com.kakastudio.sms.grade.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.grade.domain.StandardGradeScale;
import com.kakastudio.sms.grade.dto.GradeEntry;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.mapper.SectionMapper;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.service.SemesterLock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GradingServiceImplTest {

  private final SectionRepository sections = mock(SectionRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final ProfessorRepository professors = mock(ProfessorRepository.class);
  private final SemesterLock semesterLock = mock(SemesterLock.class);
  private GradingServiceImpl service;

  private final Professor okafor = professor(1L, "Okafor");
  private final Semester term = semester(2L, "2026FA", SemesterStatus.STARTED);
  private final Section cs101a = section(10L, course(1L, "CS101", 3), term, okafor, "A");
  private final Enrollment lena =
      enrollment(7L, student(1L), cs101a, EnrollmentStatus.ENROLLED, null);
  private final Long okaforUserId = okafor.getUser().getId();

  @BeforeEach
  void setUp() {
    service =
        new GradingServiceImpl(
            sections,
            enrollments,
            professors,
            new StandardGradeScale(),
            new SectionMapper(),
            semesterLock);
    when(professors.findByUserId(okaforUserId)).thenReturn(Optional.of(okafor));
    when(sections.findById(10L)).thenReturn(Optional.of(cs101a));
    when(enrollments.findById(7L)).thenReturn(Optional.of(lena));
    when(enrollments.findRoster(10L)).thenReturn(List.of(lena));
  }

  @Test
  void recordsTheScoreWithItsLetterAndPoints() {
    service.grade(okaforUserId, 10L, List.of(new GradeEntry(7L, 91)));

    verify(semesterLock).forUse(term);
    assertThat(lena.getScore()).isEqualTo(91);
    assertThat(lena.getLetterGrade()).isEqualTo("A-");
    assertThat(lena.getGradePoints()).isEqualByComparingTo("3.7");
  }

  @Test
  void anEmptyScoreClearsTheGrade() {
    lena.setScore(80);
    lena.setLetterGrade("B-");

    service.grade(okaforUserId, 10L, List.of(new GradeEntry(7L, null)));

    assertThat(lena.getScore()).isNull();
    assertThat(lena.getLetterGrade()).isNull();
    assertThat(lena.getGradePoints()).isNull();
  }

  @Test
  void gradesAreEnteredOnlyWhileTheSemesterRuns() {
    term.setStatus(SemesterStatus.READY);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.grade(okaforUserId, 10L, List.of(new GradeEntry(7L, 91))));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADING_CLOSED);
  }

  @Test
  void aProfessorGradesOnlyTheirOwnSections() {
    Professor other = professor(2L, "Reyes");
    when(professors.findByUserId(other.getUser().getId())).thenReturn(Optional.of(other));

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.grade(other.getUser().getId(), 10L, List.of(new GradeEntry(7L, 91))));

    assertThat(error.code()).isEqualTo(ErrorCode.NOT_YOUR_SECTION);
    assertThat(error.status()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void refusesAStudentWhoWithdrew() {
    lena.setStatus(EnrollmentStatus.WITHDRAWN);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.grade(okaforUserId, 10L, List.of(new GradeEntry(7L, 91))));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADE_TARGET_INVALID);
  }

  @Test
  void refusesAnEnrollmentOfAnotherSection() {
    Section other = section(11L, course(2L, "CS102", 3), term, okafor, "A");
    Enrollment elsewhere = enrollment(8L, student(2L), other, EnrollmentStatus.ENROLLED, null);
    when(enrollments.findById(8L)).thenReturn(Optional.of(elsewhere));

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () -> service.grade(okaforUserId, 10L, List.of(new GradeEntry(8L, 91))));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADE_TARGET_INVALID);
  }

  @Test
  void aDraftSectionIsInvisibleEvenToItsProfessor() {
    term.setStatus(SemesterStatus.DRAFT);

    ApiException error =
        catchThrowableOfType(ApiException.class, () -> service.mySection(okaforUserId, 10L));

    assertThat(error.code()).isEqualTo(ErrorCode.SECTION_NOT_FOUND);
  }
}
