package com.kakastudio.sms.student.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.NotFoundException;
import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.email.service.EmailComposer;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
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
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

  private final StudentRepository students;
  private final UserAccountService accounts;
  private final StudentNumberGenerator numbers;
  private final TemporaryPasswordGenerator temporaryPasswords;
  private final EmailComposer emails;
  private final EmailOutboxService outbox;
  private final StudentMapper mapper;
  private final Clock clock;

  public StudentServiceImpl(
      StudentRepository students,
      UserAccountService accounts,
      StudentNumberGenerator numbers,
      TemporaryPasswordGenerator temporaryPasswords,
      EmailComposer emails,
      EmailOutboxService outbox,
      StudentMapper mapper,
      Clock clock) {
    this.students = students;
    this.accounts = accounts;
    this.numbers = numbers;
    this.temporaryPasswords = temporaryPasswords;
    this.emails = emails;
    this.outbox = outbox;
    this.mapper = mapper;
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
    outbox.enqueue(emails.registrationReceived(user, student.getStudentNo()));
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
    outbox.enqueue(emails.registrationReceived(user, student.getStudentNo()));
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
    outbox.enqueue(emails.accountCreated(user, temporaryPassword));
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
    outbox.enqueue(emails.accountApproved(student.getUser()));
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse reject(Long id) {
    Student student = changeStatus(id, UserStatus.PENDING, UserStatus.REJECTED);
    outbox.enqueue(emails.accountRejected(student.getUser()));
    return mapper.toResponse(student);
  }

  @Override
  @Transactional
  public StudentResponse deactivate(Long id) {
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
    students.delete(student);
    accounts.delete(student.getUser());
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
