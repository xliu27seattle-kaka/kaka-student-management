package com.kakastudio.sms.graduation.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.enrollment.domain.Enrollment;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.graduation.domain.ApplicationStatus;
import com.kakastudio.sms.graduation.domain.GraduationApplication;
import com.kakastudio.sms.graduation.dto.ApplicationDto;
import com.kakastudio.sms.graduation.dto.GraduationStatusResponse;
import com.kakastudio.sms.graduation.repository.GraduationApplicationRepository;
import com.kakastudio.sms.graduation.service.CertificateNumberGenerator;
import com.kakastudio.sms.graduation.service.GraduationService;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.transcript.service.TranscriptService;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraduationServiceImpl implements GraduationService {

  /** Earned credits needed to graduate. There is no minimum GPA. */
  public static final int REQUIRED_CREDITS = 30;

  private final StudentRepository students;
  private final GraduationApplicationRepository applications;
  private final EnrollmentRepository enrollments;
  private final TranscriptService transcripts;
  private final CertificateNumberGenerator certificates;
  private final EmailComposer emails;
  private final EmailOutboxService outbox;
  private final Clock clock;

  public GraduationServiceImpl(
      StudentRepository students,
      GraduationApplicationRepository applications,
      EnrollmentRepository enrollments,
      TranscriptService transcripts,
      CertificateNumberGenerator certificates,
      EmailComposer emails,
      EmailOutboxService outbox,
      Clock clock) {
    this.students = students;
    this.applications = applications;
    this.enrollments = enrollments;
    this.transcripts = transcripts;
    this.certificates = certificates;
    this.emails = emails;
    this.outbox = outbox;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public GraduationStatusResponse status(Long studentId) {
    return statusOf(findStudent(studentId));
  }

  @Override
  @Transactional
  public GraduationStatusResponse apply(Long studentId) {
    Student student =
        students
            .findWithLockById(studentId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
    List<String> reasons = reasons(student, transcripts.earnedCredits(studentId));
    if (!reasons.isEmpty()) {
      throw new RuleViolationException(ErrorCode.GRADUATION_NOT_ELIGIBLE, reasons.getFirst());
    }
    applications.save(new GraduationApplication(student, clock.instant()));
    return statusOf(student);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ApplicationDto> applications(ApplicationStatus status) {
    return applications.findForReview(status).stream().map(this::toDto).toList();
  }

  @Override
  @Transactional
  public ApplicationDto approve(Long applicationId) {
    GraduationApplication application = pending(applicationId);
    // then the student, as enrolling does, so no seat can be taken while they graduate
    Student student =
        students
            .findWithLockById(application.getStudent().getId())
            .orElseThrow(
                () -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
    int earned = transcripts.earnedCredits(student.getId());
    if (earned < REQUIRED_CREDITS) {
      throw new RuleViolationException(
          ErrorCode.GRADUATION_NOT_ELIGIBLE,
          "The student has " + earned + " of " + REQUIRED_CREDITS + " credits");
    }
    if (enrollments.existsByStudent_IdAndStatusAndSection_Semester_Status(
        student.getId(), EnrollmentStatus.ENROLLED, SemesterStatus.STARTED)) {
      throw new RuleViolationException(
          ErrorCode.GRADUATION_NOT_ELIGIBLE,
          "The student is still enrolled in a running semester; decide after it ends");
    }
    Instant now = clock.instant();
    student.setAcademicStatus(AcademicStatus.GRADUATED);
    student.setCertificateNo(certificates.next(Year.now(clock).getValue()));
    student.setGraduatedAt(now);
    for (Enrollment upcoming :
        enrollments.findByStudent_IdAndStatusAndSection_Semester_Status(
            student.getId(), EnrollmentStatus.ENROLLED, SemesterStatus.READY)) {
      upcoming.setStatus(EnrollmentStatus.DROPPED);
    }
    application.setStatus(ApplicationStatus.APPROVED);
    application.setDecidedAt(now);
    outbox.enqueue(emails.graduationApproved(student.getUser(), student.getCertificateNo()));
    return toDto(application);
  }

  @Override
  @Transactional
  public ApplicationDto reject(Long applicationId) {
    GraduationApplication application = pending(applicationId);
    application.setStatus(ApplicationStatus.REJECTED);
    application.setDecidedAt(clock.instant());
    outbox.enqueue(emails.graduationRejected(application.getStudent().getUser()));
    return toDto(application);
  }

  private List<String> reasons(Student student, int earned) {
    List<String> reasons = new ArrayList<>();
    if (student.getAcademicStatus() == AcademicStatus.GRADUATED) {
      reasons.add("Already graduated");
      return reasons;
    }
    if (applications.existsByStudentIdAndStatus(student.getId(), ApplicationStatus.PENDING)) {
      reasons.add("Your application is waiting for a decision");
    }
    if (earned < REQUIRED_CREDITS) {
      reasons.add((REQUIRED_CREDITS - earned) + " more credits needed");
    }
    if (enrollments.existsByStudent_IdAndStatusAndSection_Semester_Status(
        student.getId(), EnrollmentStatus.ENROLLED, SemesterStatus.STARTED)) {
      reasons.add("Finish the running semester first");
    }
    return reasons;
  }

  private GraduationStatusResponse statusOf(Student student) {
    int earned = transcripts.earnedCredits(student.getId());
    List<String> reasons = reasons(student, earned);
    return new GraduationStatusResponse(
        student.getId(),
        student.getStudentNo(),
        student.getUser().getFirstName(),
        student.getUser().getLastName(),
        student.getAcademicStatus(),
        earned,
        REQUIRED_CREDITS,
        reasons.isEmpty(),
        reasons,
        applications
            .findFirstByStudentIdOrderByAppliedAtDesc(student.getId())
            .map(this::toDto)
            .orElse(null),
        student.getCertificateNo(),
        student.getGraduatedAt());
  }

  private GraduationApplication pending(Long id) {
    GraduationApplication application =
        applications
            .findWithLockById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    ErrorCode.GRADUATION_APPLICATION_NOT_FOUND, "Application not found"));
    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new ConflictException(
          ErrorCode.INVALID_STATUS_TRANSITION, "This application was already decided");
    }
    return application;
  }

  private ApplicationDto toDto(GraduationApplication application) {
    Student student = application.getStudent();
    return new ApplicationDto(
        application.getId(),
        student.getId(),
        student.getStudentNo(),
        student.getUser().fullName(),
        student.getUser().getEmail(),
        application.getStatus(),
        application.getAppliedAt(),
        application.getDecidedAt(),
        transcripts.earnedCredits(student.getId()));
  }

  private Student findStudent(Long id) {
    return students
        .findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found"));
  }
}
