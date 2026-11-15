package com.kakastudio.sms.semester.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.semester;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.semester.dto.SemesterRequest;
import com.kakastudio.sms.semester.dto.SemesterResponse;
import com.kakastudio.sms.semester.repository.SemesterRepository;
import com.kakastudio.sms.semester.service.SemesterLock;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SemesterServiceImplTest {

  private final SemesterRepository semesters = mock(SemesterRepository.class);
  private final SectionRepository sections = mock(SectionRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final SemesterLock semesterLock = mock(SemesterLock.class);
  private SemesterServiceImpl service;

  @BeforeEach
  void setUp() {
    when(semesters.save(any())).thenAnswer(call -> call.getArgument(0));
    service =
        new SemesterServiceImpl(
            semesters,
            sections,
            enrollments,
            semesterLock,
            Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC));
  }

  private Semester stored(SemesterStatus status) {
    Semester semester = semester(5L, "2027SP", status);
    when(semesters.findById(5L)).thenReturn(Optional.of(semester));
    return semester;
  }

  @Test
  void createsADraftSemester() {
    SemesterResponse created =
        service.create(
            new SemesterRequest(
                "2027sp", "Spring 2027", LocalDate.of(2027, 1, 11), LocalDate.of(2027, 5, 7)));

    assertThat(created.code()).isEqualTo("2027SP");
    assertThat(created.status()).isEqualTo(SemesterStatus.DRAFT);
  }

  @Test
  void refusesADuplicateCode() {
    when(semesters.existsByCode("2027SP")).thenReturn(true);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.create(
                    new SemesterRequest(
                        "2027SP", "Spring", LocalDate.of(2027, 1, 11), LocalDate.of(2027, 5, 7))));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_CODE_TAKEN);
  }

  @Test
  void refusesAnEndBeforeTheStart() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.create(
                    new SemesterRequest(
                        "2027SP", "Spring", LocalDate.of(2027, 5, 7), LocalDate.of(2027, 1, 11))));

    assertThat(error.code()).isEqualTo(ErrorCode.INVALID_DATE_RANGE);
  }

  @Test
  void opensADraftForEnrollment() {
    Semester semester = stored(SemesterStatus.DRAFT);

    service.transition(5L, SemesterStatus.READY);

    assertThat(semester.getStatus()).isEqualTo(SemesterStatus.READY);
    verify(semesterLock).forChange(semester);
  }

  @Test
  void refusesToSkipAStep() {
    stored(SemesterStatus.DRAFT);

    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.transition(5L, SemesterStatus.STARTED));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_TRANSITION_NOT_ALLOWED);
  }

  @Test
  void goesBackToDraftOnlyWhenNobodyIsEnrolled() {
    Semester semester = stored(SemesterStatus.READY);
    when(enrollments.countBySection_Semester_IdAndStatus(5L, EnrollmentStatus.ENROLLED))
        .thenReturn(3L);

    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.transition(5L, SemesterStatus.DRAFT));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_HAS_ENROLLMENTS);
    assertThat(semester.getStatus()).isEqualTo(SemesterStatus.READY);
  }

  @Test
  void refusesToEndWhileGradesAreMissingAndNamesTheSections() {
    Semester semester = stored(SemesterStatus.STARTED);
    when(enrollments.findUngradedSectionCodes(5L)).thenReturn(List.of("CS201-A", "MATH120-B"));

    ApiException error =
        catchThrowableOfType(
            ApiException.class, () -> service.transition(5L, SemesterStatus.ENDED));

    assertThat(error.code()).isEqualTo(ErrorCode.UNGRADED_ENROLLMENTS);
    assertThat(error.getMessage()).contains("CS201-A", "MATH120-B");
    assertThat(semester.getStatus()).isEqualTo(SemesterStatus.STARTED);
    verify(enrollments, never()).completeEnrolled(any(), any(), any(), any());
  }

  @Test
  void endingCompletesEveryEnrollment() {
    Semester semester = stored(SemesterStatus.STARTED);
    when(enrollments.findUngradedSectionCodes(5L)).thenReturn(List.of());

    service.transition(5L, SemesterStatus.ENDED);

    assertThat(semester.getStatus()).isEqualTo(SemesterStatus.ENDED);
    verify(enrollments)
        .completeEnrolled(
            eq(5L), eq(EnrollmentStatus.ENROLLED), eq(EnrollmentStatus.COMPLETED), any());
  }

  @Test
  void deletesOnlyAnEmptyDraft() {
    Semester semester = stored(SemesterStatus.DRAFT);
    when(sections.countBySemesterId(5L)).thenReturn(1L);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.delete(5L));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_IN_USE);
    verify(semesters, never()).delete(semester);
  }

  @Test
  void anEndedSemesterCannotBeEdited() {
    stored(SemesterStatus.ENDED);

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.update(
                    5L,
                    new SemesterRequest(
                        "2027SP", "Renamed", LocalDate.of(2027, 1, 11), LocalDate.of(2027, 5, 7))));

    assertThat(error.code()).isEqualTo(ErrorCode.SEMESTER_NOT_EDITABLE);
  }

  @Test
  void listsTheSectionsStillMissingGrades() {
    stored(SemesterStatus.STARTED);
    when(enrollments.findUngradedSectionCodes(5L)).thenReturn(List.of("CS301-A"));

    assertThat(service.ungradedSections(5L)).containsExactly("CS301-A");
  }
}
