package com.kakastudio.sms.transcript.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.graded;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.course.domain.Course;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.grade.domain.StandardGradeScale;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.semester.domain.Semester;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.transcript.dto.RowStatus;
import com.kakastudio.sms.transcript.dto.TranscriptResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TranscriptServiceImplTest {

  private final StudentRepository students = mock(StudentRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private TranscriptServiceImpl service;

  private final Student lena = student(1L);
  private final Professor okafor = professor(1L, "Okafor");
  private final Course cs101 = course(1L, "CS101", 3);
  private final Course math120 = course(2L, "MATH120", 4);
  private final Course cs102 = course(3L, "CS102", 3);
  private final Course cs201 = course(4L, "CS201", 3);
  private final Semester spring = semester(1L, "2026SP", SemesterStatus.ENDED);
  private final Semester fall = semester(2L, "2026FA", SemesterStatus.ENDED);
  private final Semester next = semester(3L, "2027SP", SemesterStatus.READY);

  @BeforeEach
  void setUp() {
    spring.setStartDate(LocalDate.of(2026, 1, 12));
    fall.setStartDate(LocalDate.of(2026, 8, 24));
    next.setStartDate(LocalDate.of(2027, 1, 11));
    service = new TranscriptServiceImpl(students, enrollments, new StandardGradeScale());
    when(students.findById(1L)).thenReturn(Optional.of(lena));
    when(enrollments.findHistory(1L))
        .thenReturn(
            List.of(
                enrollment(
                    5L,
                    lena,
                    section(15L, cs201, next, okafor, "A"),
                    EnrollmentStatus.ENROLLED,
                    null),
                graded(1L, lena, section(11L, cs101, spring, okafor, "A"), 95, "A", "4.0"),
                graded(2L, lena, section(12L, math120, spring, okafor, "A"), 40, "F", "0.0"),
                enrollment(
                    3L,
                    lena,
                    section(13L, cs102, spring, okafor, "A"),
                    EnrollmentStatus.WITHDRAWN,
                    null),
                enrollment(
                    6L,
                    lena,
                    section(16L, cs102, fall, okafor, "B"),
                    EnrollmentStatus.DROPPED,
                    null),
                graded(4L, lena, section(14L, math120, fall, okafor, "B"), 85, "B", "3.0")));
  }

  @Test
  void groupsAttemptsBySemesterInDateOrder() {
    TranscriptResponse transcript = service.transcript(1L);

    assertThat(transcript.terms())
        .extracting("semesterCode")
        .containsExactly("2026SP", "2026FA", "2027SP");
    assertThat(transcript.terms().get(0).rows())
        .extracting("courseCode")
        .containsExactly("CS101", "CS102", "MATH120");
  }

  @Test
  void droppedSectionsLeaveNoTraceAndWithdrawalsShowAsW() {
    TranscriptResponse transcript = service.transcript(1L);

    assertThat(transcript.terms().get(1).rows())
        .extracting("courseCode")
        .containsExactly("MATH120");
    assertThat(transcript.terms().get(0).rows().get(1).status()).isEqualTo(RowStatus.WITHDRAWN);
    assertThat(transcript.terms().get(0).rows().get(1).letter()).isEqualTo("W");
  }

  @Test
  void bothAttemptsOfARetakeCountAndFailuresCountAsZero() {
    TranscriptResponse transcript = service.transcript(1L);

    // (3 x 4.0 + 4 x 0.0 + 4 x 3.0) / 11 = 2.18; W and in-progress rows are left out
    assertThat(transcript.cumulativeGpa()).isEqualByComparingTo("2.18");
    assertThat(transcript.terms().get(0).gpa()).isEqualByComparingTo("1.71");
    assertThat(transcript.terms().get(1).gpa()).isEqualByComparingTo("3.00");
    assertThat(transcript.terms().get(2).gpa()).isNull();
    assertThat(transcript.terms().get(2).rows().get(0).status()).isEqualTo(RowStatus.IN_PROGRESS);
  }

  @Test
  void earnedCreditsCountEachPassedCourseOnce() {
    assertThat(service.transcript(1L).earnedCredits()).isEqualTo(7);
    assertThat(service.earnedCredits(1L)).isEqualTo(7);
  }

  @Test
  void aStudentWithoutGradesHasNoGpa() {
    when(enrollments.findHistory(1L)).thenReturn(List.of());

    TranscriptResponse transcript = service.transcript(1L);

    assertThat(transcript.cumulativeGpa()).isNull();
    assertThat(transcript.official()).isFalse();
  }

  @Test
  void isOfficialOnceTheStudentGraduated() {
    lena.setAcademicStatus(AcademicStatus.GRADUATED);
    lena.setCertificateNo("KS-2026-0001");

    assertThat(service.transcript(1L).official()).isTrue();
  }
}
