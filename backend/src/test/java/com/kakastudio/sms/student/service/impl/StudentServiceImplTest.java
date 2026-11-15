package com.kakastudio.sms.student.service.impl;

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
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.email.service.impl.HtmlEmailComposer;
import com.kakastudio.sms.notification.message.domain.Notification;
import com.kakastudio.sms.notification.message.repository.NotificationRecipientRepository;
import com.kakastudio.sms.notification.message.repository.NotificationRepository;
import com.kakastudio.sms.notification.message.service.impl.SystemNoticesImpl;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.student.domain.AcademicStatus;
import com.kakastudio.sms.student.domain.Student;
import com.kakastudio.sms.student.dto.RegisterRequest;
import com.kakastudio.sms.student.dto.RegisterResponse;
import com.kakastudio.sms.student.dto.StudentCreateRequest;
import com.kakastudio.sms.student.dto.StudentResponse;
import com.kakastudio.sms.student.dto.StudentUpdateRequest;
import com.kakastudio.sms.student.mapper.StudentMapper;
import com.kakastudio.sms.student.repository.StudentRepository;
import com.kakastudio.sms.student.service.StudentNumberGenerator;
import com.kakastudio.sms.testsupport.Catalog;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import com.kakastudio.sms.user.service.TemporaryPasswordGenerator;
import com.kakastudio.sms.user.service.impl.UserAccountServiceImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class StudentServiceImplTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  private final StudentRepository students = mock(StudentRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final StudentNumberGenerator numbers = mock(StudentNumberGenerator.class);
  private final TemporaryPasswordGenerator temporaryPasswords =
      mock(TemporaryPasswordGenerator.class);
  private final EmailOutboxService outbox = mock(EmailOutboxService.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final NotificationRepository notifications = mock(NotificationRepository.class);
  private final NotificationRecipientRepository recipients =
      mock(NotificationRecipientRepository.class);
  private StudentServiceImpl service;

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode(any())).thenAnswer(call -> "encoded:" + call.getArgument(0));
    when(users.save(any())).thenAnswer(call -> call.getArgument(0));
    when(students.save(any())).thenAnswer(call -> call.getArgument(0));
    when(numbers.next(2026)).thenReturn("S2026-0001");
    when(temporaryPasswords.generate()).thenReturn("Temp1234abcd");
    when(notifications.save(any())).thenAnswer(call -> call.getArgument(0));
    when(outbox.enqueue(any())).thenReturn(99L);
    service =
        new StudentServiceImpl(
            students,
            new UserAccountServiceImpl(users, passwordEncoder),
            numbers,
            temporaryPasswords,
            new HtmlEmailComposer(),
            new SystemNoticesImpl(
                outbox, notifications, recipients, users, Clock.fixed(NOW, ZoneOffset.UTC)),
            new StudentMapper(),
            enrollments,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private Student stored(Long id, UserStatus status) {
    User user = Users.user(100L + id, "lena.fischer@uni.example", Role.STUDENT, status);
    Student student = new Student(user, "S2026-0001");
    student.setId(id);
    when(students.findById(id)).thenReturn(Optional.of(student));
    when(students.findByUserId(user.getId())).thenReturn(Optional.of(student));
    return student;
  }

  private EmailMessage enqueuedEmail() {
    ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(outbox).enqueue(captor.capture());
    return captor.getValue();
  }

  private ApiException failure(Runnable action) {
    return catchThrowableOfType(ApiException.class, action::run);
  }

  @Test
  void registerCreatesAPendingStudentAndQueuesTheEmail() {
    when(users.findByEmail("lena.fischer@uni.example")).thenReturn(Optional.empty());

    RegisterResponse response =
        service.register(
            new RegisterRequest("  Lena.Fischer@Uni.Example ", " Lena ", "Fischer", "Passw0rd1"));

    assertThat(response).isEqualTo(new RegisterResponse("S2026-0001", UserStatus.PENDING));
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo("lena.fischer@uni.example");
    assertThat(saved.getValue().getFirstName()).isEqualTo("Lena");
    assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
    assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
    assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded:Passw0rd1");
    assertThat(enqueuedEmail().subject()).isEqualTo("We received your registration");
  }

  @Test
  void registerRefusesAnEmailThatDiffersOnlyByCaseOrSpaces() {
    when(users.findByEmail("lena.fischer@uni.example"))
        .thenReturn(Optional.of(Users.activeStudent(7L)));

    ApiException thrown =
        failure(() ->
            service.register(
                new RegisterRequest(" LENA.Fischer@uni.example", "Lena", "Fischer", "Passw0rd1")));

    assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(thrown.code()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    verify(users, never()).save(any());
    verify(outbox, never()).enqueue(any());
  }

  @Test
  void rejectedApplicantCanRegisterAgainAndKeepsTheStudentNumber() {
    Student student = stored(3L, UserStatus.REJECTED);
    when(users.findByEmail("lena.fischer@uni.example")).thenReturn(Optional.of(student.getUser()));

    RegisterResponse response =
        service.register(
            new RegisterRequest("lena.fischer@uni.example", "Helena", "Fischer", "NewPassw0rd2"));

    assertThat(response).isEqualTo(new RegisterResponse("S2026-0001", UserStatus.PENDING));
    assertThat(student.getUser().getStatus()).isEqualTo(UserStatus.PENDING);
    assertThat(student.getUser().getFirstName()).isEqualTo("Helena");
    assertThat(student.getUser().getPasswordHash()).isEqualTo("encoded:NewPassw0rd2");
    verify(numbers, never()).next(2026);
    assertThat(enqueuedEmail().subject()).isEqualTo("We received your registration");
  }

  @Test
  void approveActivatesAPendingStudentAndQueuesTheEmail() {
    Student student = stored(3L, UserStatus.PENDING);

    StudentResponse response = service.approve(3L);

    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(student.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(enqueuedEmail().subject()).isEqualTo("Your account is approved");
  }

  @Test
  void approvingTwiceIsAConflictNotACrash() {
    stored(3L, UserStatus.ACTIVE);

    ApiException thrown = failure(() -> service.approve(3L));

    assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(thrown.code()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    verify(outbox, never()).enqueue(any());
  }

  @Test
  void rejectMarksRejectedAndQueuesTheEmail() {
    Student student = stored(3L, UserStatus.PENDING);

    service.reject(3L);

    assertThat(student.getUser().getStatus()).isEqualTo(UserStatus.REJECTED);
    assertThat(enqueuedEmail().subject()).isEqualTo("Your registration was not approved");
  }

  @Test
  void rejectingAnActiveStudentIsAConflict() {
    stored(3L, UserStatus.ACTIVE);

    assertThat(failure(() -> service.reject(3L)).code())
        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  }

  @Test
  void deactivateAndReactivateFollowTheAllowedOrder() {
    Student student = stored(3L, UserStatus.ACTIVE);

    assertThat(service.deactivate(3L).status()).isEqualTo(UserStatus.INACTIVE);
    assertThat(failure(() -> service.deactivate(3L)).code())
        .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    assertThat(service.reactivate(3L).status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(student.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void createMakesAnActiveStudentWithATemporaryPassword() {
    when(users.findByEmail("omar.haddad@uni.example")).thenReturn(Optional.empty());

    StudentResponse response =
        service.create(new StudentCreateRequest("Omar.Haddad@uni.example", "Omar", "Haddad"));

    assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    assertThat(response.studentNo()).isEqualTo("S2026-0001");
    assertThat(response.academicStatus()).isEqualTo(AcademicStatus.ENROLLED);
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users).save(saved.capture());
    assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded:Temp1234abcd");
    EmailMessage email = enqueuedEmail();
    assertThat(email.subject()).isEqualTo("Your Kaka Student Management account");
    assertThat(email.html()).contains("Temp1234abcd");
  }

  @Test
  void updateToAnotherAccountsEmailIsAConflict() {
    stored(3L, UserStatus.ACTIVE);
    when(users.existsByEmail("taken@uni.example")).thenReturn(true);

    ApiException thrown =
        failure(
            () -> service.update(3L, new StudentUpdateRequest("taken@uni.example", "Lena", "F")));

    assertThat(thrown.code()).isEqualTo(ErrorCode.EMAIL_TAKEN);
  }

  @Test
  void updateKeepingTheSameEmailIsAllowed() {
    Student student = stored(3L, UserStatus.ACTIVE);
    when(users.existsByEmail("lena.fischer@uni.example")).thenReturn(true);

    StudentResponse response =
        service.update(
            3L, new StudentUpdateRequest("lena.fischer@uni.example", "Helena", "Fischer"));

    assertThat(response.firstName()).isEqualTo("Helena");
    assertThat(student.getUser().getFirstName()).isEqualTo("Helena");
  }

  @Test
  void unknownIdIsNotFound() {
    when(students.findById(99L)).thenReturn(Optional.empty());

    ApiException thrown = failure(() -> service.get(99L));

    assertThat(thrown.status()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(thrown.code()).isEqualTo(ErrorCode.STUDENT_NOT_FOUND);
  }

  @Test
  @SuppressWarnings("unchecked")
  void listCapsThePageSizeAt100() {
    when(students.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    PageResponse<StudentResponse> page = service.list(null, null, 0, 500);

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(students).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    assertThat(page.items()).isEmpty();
  }

  @Test
  void deleteRemovesTheStudentAndTheAccount() {
    Student student = stored(3L, UserStatus.INACTIVE);

    service.delete(3L);

    verify(students).delete(student);
    verify(users).delete(student.getUser());
  }

  @Test
  void aStudentWithAnOpenOrRunningSeatCannotBeDeactivated() {
    Student student = stored(3L, UserStatus.ACTIVE);
    Section section =
        Catalog.section(
            9L,
            Catalog.course(1L, "CS201", 3),
            Catalog.semester(2L, "2027SP", SemesterStatus.READY),
            Catalog.professor(1L, "Okafor"),
            "A");
    when(enrollments.findByStudent_IdAndStatusAndSection_Semester_StatusIn(
            eq(3L), eq(EnrollmentStatus.ENROLLED), any()))
        .thenReturn(
            List.of(Catalog.enrollment(1L, student, section, EnrollmentStatus.ENROLLED, null)));

    ApiException error = failure(() -> service.deactivate(3L));

    assertThat(error.code()).isEqualTo(ErrorCode.HAS_ACTIVE_COMMITMENTS);
    assertThat(error.getMessage()).contains("CS201-A");
    assertThat(student.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  void aStudentWithHistoryCannotBeDeleted() {
    Student student = stored(3L, UserStatus.INACTIVE);
    when(enrollments.existsByStudentIdAndStatusNot(3L, EnrollmentStatus.DROPPED)).thenReturn(true);

    ApiException error = failure(() -> service.delete(3L));

    assertThat(error.code()).isEqualTo(ErrorCode.HAS_HISTORY);
    verify(students, never()).delete(student);
  }

  @Test
  void droppedSectionsLeaveNoTraceSoTheStudentCanStillBeDeleted() {
    Student student = stored(3L, UserStatus.INACTIVE);
    when(enrollments.existsByStudentIdAndStatusNot(3L, EnrollmentStatus.DROPPED)).thenReturn(false);

    service.delete(3L);

    verify(enrollments).deleteByStudentId(3L);
    verify(students).delete(student);
  }

  private List<Notification> savedNotes() {
    ArgumentCaptor<Notification> notes = ArgumentCaptor.forClass(Notification.class);
    verify(notifications, org.mockito.Mockito.atLeastOnce()).save(notes.capture());
    return notes.getAllValues();
  }

  @Test
  void approvalAlsoLandsInTheStudentsInbox() {
    stored(3L, UserStatus.PENDING);

    service.approve(3L);

    assertThat(savedNotes())
        .extracting(Notification::getSubject)
        .contains("Your account is approved");
  }

  @Test
  void aRegistrationAlertsTheAdministrators() {
    when(users.findByEmail("omar.haddad@uni.example")).thenReturn(Optional.empty());
    when(users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE))
        .thenReturn(List.of(Users.activeAdmin(1L)));

    service.register(new RegisterRequest("omar.haddad@uni.example", "Omar", "Haddad", "Passw0rd1"));

    assertThat(savedNotes())
        .extracting(Notification::getSubject)
        .contains("We received your registration", "New registration: Omar Haddad");
  }

  @Test
  void theInboxNoteOfANewAccountNeverRepeatsTheTemporaryPassword() {
    when(users.findByEmail("omar.haddad@uni.example")).thenReturn(Optional.empty());

    service.create(new StudentCreateRequest("omar.haddad@uni.example", "Omar", "Haddad"));

    assertThat(savedNotes())
        .extracting(Notification::getBody)
        .noneMatch(body -> body.contains("Temp1234abcd"));
  }
}
