package com.kakastudio.sms.graduation.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.domain.GraduationApplication;
import com.kakastudio.sms.graduation.dto.GraduationStatusResponse;
import com.kakastudio.sms.graduation.repository.GraduationApplicationRepository;
import com.kakastudio.sms.graduation.service.CertificateNumberGenerator;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.email.service.impl.HtmlEmailComposer;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.transcript.service.TranscriptService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class GraduationServiceImplTest {

  private final StudentRepository students = mock(StudentRepository.class);
  private final GraduationApplicationRepository applications =
      mock(GraduationApplicationRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final TranscriptService transcripts = mock(TranscriptService.class);
  private final CertificateNumberGenerator certificates = mock(CertificateNumberGenerator.class);
  private final EmailOutboxService outbox = mock(EmailOutboxService.class);
  private GraduationServiceImpl service;

  private final Student lena = student(1L);

  @BeforeEach
  void setUp() {
    service =
        new GraduationServiceImpl(
            students,
            applications,
            enrollments,
            transcripts,
            certificates,
            new HtmlEmailComposer(),
            outbox,
            Clock.fixed(Instant.parse("2026-12-20T10:00:00Z"), ZoneOffset.UTC));
    when(students.findById(1L)).thenReturn(Optional.of(lena));
    when(students.findWithLockById(1L)).thenReturn(Optional.of(lena));
    when(applications.save(any())).thenAnswer(call -> call.getArgument(0));
    when(certificates.next(2026)).thenReturn("KS-2026-0001");
  }

  @Test
  void belowThirtyCreditsShowsWhatIsMissing() {
    when(transcripts.earnedCredits(1L)).thenReturn(12);

    GraduationStatusResponse status = service.status(1L);

    assertThat(status.eligible()).isFalse();
    assertThat(status.earnedCredits()).isEqualTo(12);
    assertThat(status.requiredCredits()).isEqualTo(30);
    assertThat(status.reasons()).containsExactly("18 more credits needed");
  }

  @Test
  void applyingWithThirtyCreditsCreatesAPendingApplication() {
    when(transcripts.earnedCredits(1L)).thenReturn(30);

    GraduationStatusResponse status = service.apply(1L);

    ArgumentCaptor<GraduationApplication> saved =
        ArgumentCaptor.forClass(GraduationApplication.class);
    verify(applications).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(ApplicationStatus.PENDING);
    assertThat(status).isNotNull();
  }

  @Test
  void cannotApplyWhileARunningSemesterIsUnfinished() {
    when(transcripts.earnedCredits(1L)).thenReturn(33);
    when(enrollments.existsByStudent_IdAndStatusAndSection_Semester_Status(
            1L, EnrollmentStatus.ENROLLED, SemesterStatus.STARTED))
        .thenReturn(true);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.apply(1L));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADUATION_NOT_ELIGIBLE);
    assertThat(error.getMessage()).contains("running semester");
  }

  @Test
  void cannotApplyTwice() {
    when(transcripts.earnedCredits(1L)).thenReturn(33);
    when(applications.existsByStudentIdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(true);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.apply(1L));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADUATION_NOT_ELIGIBLE);
    verify(applications, never()).save(any());
  }

  @Test
  void approvalGraduatesIssuesACertificateDropsOpenEnrollmentsAndEmails() {
    GraduationApplication application = new GraduationApplication(lena, Instant.now());
    application.setId(4L);
    when(applications.findWithLockById(4L)).thenReturn(Optional.of(application));
    when(transcripts.earnedCredits(1L)).thenReturn(31);
    Enrollment upcoming =
        enrollment(
            9L,
            lena,
            section(
                1L,
                course(1L, "CS401", 3),
                semester(3L, "2027SP", SemesterStatus.READY),
                professor(1L, "Okafor"),
                "A"),
            EnrollmentStatus.ENROLLED,
            null);
    when(enrollments.findByStudent_IdAndStatusAndSection_Semester_Status(
            1L, EnrollmentStatus.ENROLLED, SemesterStatus.READY))
        .thenReturn(List.of(upcoming));

    service.approve(4L);

    InOrder order = inOrder(applications, students);
    order.verify(applications).findWithLockById(4L);
    order.verify(students).findWithLockById(1L);
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    assertThat(lena.getAcademicStatus()).isEqualTo(AcademicStatus.GRADUATED);
    assertThat(lena.getCertificateNo()).isEqualTo("KS-2026-0001");
    assertThat(upcoming.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
    ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
    verify(outbox).enqueue(email.capture());
    assertThat(email.getValue().html()).contains("KS-2026-0001");
  }

  @Test
  void rejectionEmailsAndAllowsANewApplication() {
    GraduationApplication application = new GraduationApplication(lena, Instant.now());
    application.setId(4L);
    when(applications.findWithLockById(4L)).thenReturn(Optional.of(application));
    when(transcripts.earnedCredits(1L)).thenReturn(31);

    service.reject(4L);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    verify(outbox).enqueue(any());
    assertThat(service.status(1L).eligible()).isTrue();
  }

  @Test
  void aDecidedApplicationCannotBeDecidedAgain() {
    GraduationApplication application = new GraduationApplication(lena, Instant.now());
    application.setStatus(ApplicationStatus.REJECTED);
    when(applications.findWithLockById(4L)).thenReturn(Optional.of(application));

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.approve(4L));

    assertThat(error.code()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  }

  @Test
  void approvalIsRefusedWhileTheStudentIsInARunningSemester() {
    GraduationApplication application = new GraduationApplication(lena, Instant.now());
    when(applications.findWithLockById(4L)).thenReturn(Optional.of(application));
    when(transcripts.earnedCredits(1L)).thenReturn(31);
    when(enrollments.existsByStudent_IdAndStatusAndSection_Semester_Status(
            1L, EnrollmentStatus.ENROLLED, SemesterStatus.STARTED))
        .thenReturn(true);

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.approve(4L));

    assertThat(error.code()).isEqualTo(ErrorCode.GRADUATION_NOT_ELIGIBLE);
    assertThat(lena.getAcademicStatus()).isEqualTo(AcademicStatus.ENROLLED);
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
  }
}
