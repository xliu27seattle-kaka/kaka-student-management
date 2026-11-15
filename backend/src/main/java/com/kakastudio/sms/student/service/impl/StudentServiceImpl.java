package com.kakastudio.sms.student.service.impl;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.message.service.SystemNotices;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.dto.RegisterRequest;
import com.kakastudio.sms.student.dto.RegisterResponse;
import com.kakastudio.sms.student.dto.StudentCreateRequest;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.student.dto.StudentUpdateRequest;
import com.kakastudio.sms.student.mapper.StudentMapper;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.student.repository.StudentSpecifications;
import com.kakastudio.sms.student.service.StudentNumberGenerator;
import com.kakastudio.sms.student.service.StudentService;
import com.kakastudio.sms.user.EmailTakenException;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.service.NewAccount;
import com.kakastudio.sms.user.service.TemporaryPasswordGenerator;
import com.kakastudio.sms.user.service.UserAccountService;
import java.time.Clock;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

  /**
   * Inbox note for a new account. The email carries the temporary password; the note never does.
   */
  public static final String ACCOUNT_CREATED_NOTE =
      "An administrator created your account. A temporary password was sent to your email"
          + " address; change it from your Account page.";

  private final StudentRepository students;
  private final UserAccountService accounts;
  private final StudentNumberGenerator numbers;
  private final TemporaryPasswordGenerator temporaryPasswords;
  private final EmailComposer emails;
  private final SystemNotices notices;
  private final StudentMapper mapper;
  private final EnrollmentRepository enrollments;
  private final Clock clock;

  public StudentServiceImpl(
      StudentRepository students,
      UserAccountService accounts,
      StudentNumberGenerator numbers,
      TemporaryPasswordGenerator temporaryPasswords,
      EmailComposer emails,
      SystemNotices notices,
      StudentMapper mapper,
      EnrollmentRepository enrollments,
      Clock clock) {
    this.students = students;
    this.accounts = accounts;
    this.numbers = numbers;
    this.temporaryPasswords = temporaryPasswords;
    this.emails = emails;
    this.notices = notices;
    this.mapper = mapper;
    this.enrollments = enrollments;
    this.clock = clock;
  }

  @Override
  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    Optional<User> existing = accounts.findByEmail(request.email());
    if (existing.isPresent()) {
      return registerAgain(existing.get(), request);
    }
    User user =
        accounts.create(
            new NewAccount(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                Role.STUDENT,
                UserStatus.PENDING));
    Student student = students.save(new Student(user, nextStudentNumber()));
    announceRegistration(user, student);
    return new RegisterResponse(student.getStudentNo(), user.getStatus());
  }

  /** Only an applicant who was rejected may use the same email again; they keep their number. */
  private RegisterResponse registerAgain(User user, RegisterRequest request) {
    if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.REJECTED) {
      throw new EmailTakenException();
    }
    Student student = students.findByUserId(user.getId()).orElseThrow(this::notFound);
    accounts.changeDetails(user, user.getEmail(), request.firstName(), request.lastName());
    accounts.changePassword(user, request.password());
    accounts.changeStatus(user, UserStatus.REJECTED, UserStatus.PENDING);
    announceRegistration(user, student);
    return new RegisterResponse(student.getStudentNo(), user.getStatus());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<StudentResponse> list(UserStatus status, String query, int page, int size) {
    return PageResponse.of(
        students.findAll(
            StudentSpecifications.matching(status, query), PageRequests.newestFirst(page, size)),
        mapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public StudentResponse get(Long id) {
    return mapper.toResponse(find(id));
  }

  @Override
  @Transactional
  public StudentResponse create(StudentCreateRequest request) {
    String temporaryPassword = temporaryPasswords.generate();
    User user =
        accounts.create(
            new NewAccount(
                request.email(),
                request.firstName(),
                request.lastName(),
                temporaryPassword,
                Role.STUDENT,
                UserStatus.ACTIVE));
    Student student = students.save(new Student(user, nextStudentNumber()));
    notices.send(user, emails.accountCreated(user, temporaryPassword), ACCOUNT_CREATED_NOTE);
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse update(Long id, StudentUpdateRequest request) {
    Student student = find(id);
    accounts.changeDetails(
        student.getUser(), request.email(), request.firstName(), request.lastName());
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse approve(Long id) {
    Student student = changeStatus(id, UserStatus.PENDING, UserStatus.ACTIVE);
    notices.send(
        student.getUser(),
        emails.accountApproved(student.getUser()),
        "Your registration was approved. Welcome to Kaka Student Management.");
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse reject(Long id) {
    Student student = changeStatus(id, UserStatus.PENDING, UserStatus.REJECTED);
    notices.send(
        student.getUser(),
        emails.accountRejected(student.getUser()),
        "Your registration was not approved. You can register again with the same email address.");
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse deactivate(Long id) {
    List<String> seats =
        enrollments
            .findByStudent_IdAndStatusAndSection_Semester_StatusIn(
                id,
                EnrollmentStatus.ENROLLED,
                EnumSet.of(SemesterStatus.READY, SemesterStatus.STARTED))
            .stream()
            .map(e -> e.getSection().code())
            .sorted()
            .toList();
    if (!seats.isEmpty()) {
      throw new ConflictException(
          ErrorCode.HAS_ACTIVE_COMMITMENTS,
          "Drop the student from " + String.join(", ", seats) + " first");
    }
    return mapper.toResponse(changeStatus(id, UserStatus.ACTIVE, UserStatus.INACTIVE));
  }

  @Override
  @Transactional
  public StudentResponse reactivate(Long id) {
    return mapper.toResponse(changeStatus(id, UserStatus.INACTIVE, UserStatus.ACTIVE));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Student student = find(id);
    if (enrollments.existsByStudentIdAndStatusNot(id, EnrollmentStatus.DROPPED)) {
      throw new ConflictException(
          ErrorCode.HAS_HISTORY,
          "This student has enrollment history, so the record stays. Deactivate the account instead.");
    }
    enrollments.deleteByStudentId(id); // only dropped sections are left, and they leave no trace
    students.delete(student);
    accounts.delete(student.getUser());
  }

  /** The applicant hears that it arrived; the administrators that it waits for them. */
  private void announceRegistration(User user, Student student) {
    notices.send(
        user,
        emails.registrationReceived(user, student.getStudentNo()),
        "Your student number is "
            + student.getStudentNo()
            + ". An administrator will review your registration; you can sign in once it is"
            + " approved.");
    notices.tellAdministrators(
        "New registration: " + user.fullName(),
        user.fullName()
            + " ("
            + user.getEmail()
            + ") registered and is waiting for approval. Approve or reject it under Students.");
  }

  private Student changeStatus(Long id, UserStatus from, UserStatus to) {
    Student student = find(id);
    accounts.changeStatus(student.getUser(), from, to);
    return student;
  }

  private String nextStudentNumber() {
    return numbers.next(Year.now(clock).getValue());
  }

  private Student find(Long id) {
    return students.findById(id).orElseThrow(this::notFound);
  }

  private NotFoundException notFound() {
    return new NotFoundException(ErrorCode.STUDENT_NOT_FOUND, "Student not found");
  }
}
