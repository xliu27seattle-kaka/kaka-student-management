package com.kakastudio.sms.notification.message.service.impl;

import static com.kakastudio.sms.testsupport.Catalog.course;
import static com.kakastudio.sms.testsupport.Catalog.enrollment;
import static com.kakastudio.sms.testsupport.Catalog.professor;
import static com.kakastudio.sms.testsupport.Catalog.section;
import static com.kakastudio.sms.testsupport.Catalog.semester;
import static com.kakastudio.sms.testsupport.Catalog.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.enrollment.domain.EnrollmentStatus;
import com.kakastudio.sms.enrollment.repository.EnrollmentRepository;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.email.service.impl.HtmlEmailComposer;
import com.kakastudio.sms.notification.message.domain.Audience;
import com.kakastudio.sms.notification.message.domain.Notification;
import com.kakastudio.sms.notification.message.domain.NotificationRecipient;
import com.kakastudio.sms.notification.message.dto.AdminNotificationRequest;
import com.kakastudio.sms.notification.message.dto.NotificationRequest;
import com.kakastudio.sms.notification.message.dto.SentNotification;
import com.kakastudio.sms.notification.message.repository.NotificationRecipientRepository;
import com.kakastudio.sms.notification.message.repository.NotificationRepository;
import com.kakastudio.sms.professor.domain.Professor;
import com.kakastudio.sms.professor.repository.ProfessorRepository;
import com.kakastudio.sms.section.domain.Section;
import com.kakastudio.sms.section.repository.SectionRepository;
import com.kakastudio.sms.semester.domain.SemesterStatus;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceImplTest {

  private final NotificationRepository notifications = mock(NotificationRepository.class);
  private final NotificationRecipientRepository recipients =
      mock(NotificationRecipientRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final SectionRepository sections = mock(SectionRepository.class);
  private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
  private final ProfessorRepository professors = mock(ProfessorRepository.class);
  private final EmailOutboxService outbox = mock(EmailOutboxService.class);
  private NotificationServiceImpl service;

  private final User admin = Users.activeAdmin(1L);
  private final User student1 = Users.user(11L, "a@uni.example", Role.STUDENT, UserStatus.ACTIVE);
  private final User student2 = Users.user(12L, "b@uni.example", Role.STUDENT, UserStatus.ACTIVE);
  private final Professor okafor = professor(1L, "Okafor");

  @BeforeEach
  void setUp() {
    service =
        new NotificationServiceImpl(
            notifications,
            recipients,
            users,
            sections,
            enrollments,
            professors,
            new HtmlEmailComposer(),
            outbox,
            Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC));
    when(users.findById(1L)).thenReturn(Optional.of(admin));
    when(users.findById(okafor.getUser().getId())).thenReturn(Optional.of(okafor.getUser()));
    when(notifications.save(any())).thenAnswer(call -> call.getArgument(0));
    when(outbox.enqueue(any())).thenReturn(50L, 51L, 52L);
    when(professors.findByUserId(okafor.getUser().getId())).thenReturn(Optional.of(okafor));
  }

  @Test
  void anAdministratorReachesEveryActiveStudentAndEmailsEachOne() {
    when(users.findByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE))
        .thenReturn(List.of(student1, student2));

    SentNotification sent =
        service.sendAsAdmin(
            1L, new AdminNotificationRequest(Audience.ALL_STUDENTS, "Library hours", "Open late."));

    assertThat(sent.recipientCount()).isEqualTo(2);
    ArgumentCaptor<List<NotificationRecipient>> saved = ArgumentCaptor.captor();
    verify(recipients).saveAll(saved.capture());
    assertThat(saved.getValue()).extracting(r -> r.getUser().getId()).containsExactly(11L, 12L);
    assertThat(saved.getValue())
        .extracting(NotificationRecipient::getEmailOutboxId)
        .containsExactly(50L, 51L);
    ArgumentCaptor<EmailMessage> email = ArgumentCaptor.forClass(EmailMessage.class);
    verify(outbox, times(2)).enqueue(email.capture());
    assertThat(email.getValue().subject()).isEqualTo("Library hours");
  }

  @Test
  void everyoneMeansStudentsAndProfessors() {
    when(users.findByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE)).thenReturn(List.of(student1));
    when(users.findByRoleAndStatus(Role.PROFESSOR, UserStatus.ACTIVE))
        .thenReturn(List.of(okafor.getUser()));

    SentNotification sent =
        service.sendAsAdmin(1L, new AdminNotificationRequest(Audience.EVERYONE, "Hi", "Body"));

    assertThat(sent.recipientCount()).isEqualTo(2);
  }

  @Test
  void anAdministratorCannotPickTheSectionAudience() {
    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.sendAsAdmin(
                    1L, new AdminNotificationRequest(Audience.SECTION, "Hi", "Body")));

    assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
  }

  @Test
  void aProfessorReachesTheStudentsEnrolledInTheirSection() {
    Section cs101a =
        section(
            10L,
            course(1L, "CS101", 3),
            semester(2L, "2026FA", SemesterStatus.STARTED),
            okafor,
            "A");
    when(sections.findById(10L)).thenReturn(Optional.of(cs101a));
    when(enrollments.findBySection_IdAndStatus(10L, EnrollmentStatus.ENROLLED))
        .thenReturn(List.of(enrollment(1L, student(1L), cs101a, EnrollmentStatus.ENROLLED, null)));

    SentNotification sent =
        service.sendToSection(
            okafor.getUser().getId(), 10L, new NotificationRequest("Quiz moved", "Now on Friday."));

    assertThat(sent.recipientCount()).isEqualTo(1);
    assertThat(sent.sectionCode()).isEqualTo("CS101-A");
    ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(notification.capture());
    assertThat(notification.getValue().getAudience()).isEqualTo(Audience.SECTION);
  }

  @Test
  void aProfessorCannotWriteToAnotherProfessorsSection() {
    Section theirs =
        section(
            10L,
            course(1L, "CS101", 3),
            semester(2L, "2026FA", SemesterStatus.STARTED),
            professor(2L, "Reyes"),
            "A");
    when(sections.findById(10L)).thenReturn(Optional.of(theirs));

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.sendToSection(
                    okafor.getUser().getId(), 10L, new NotificationRequest("Hi", "Body")));

    assertThat(error.code()).isEqualTo(ErrorCode.NOT_YOUR_SECTION);
  }

  @Test
  void nobodyToTellIsAnError() {
    when(users.findByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE)).thenReturn(List.of());

    ApiException error =
        catchThrowableOfType(
            ApiException.class,
            () ->
                service.sendAsAdmin(
                    1L, new AdminNotificationRequest(Audience.ALL_STUDENTS, "Hi", "Body")));

    assertThat(error.code()).isEqualTo(ErrorCode.NO_RECIPIENTS);
  }

  @Test
  void onlyTheRecipientCanMarkAMessageRead() {
    NotificationRecipient row =
        new NotificationRecipient(
            new Notification(admin, Audience.ALL_STUDENTS, null, "Hi", "Body", Instant.now()),
            student1);
    when(recipients.findById(5L)).thenReturn(Optional.of(row));

    ApiException error = catchThrowableOfType(ApiException.class, () -> service.markRead(12L, 5L));
    assertThat(error.code()).isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);

    service.markRead(11L, 5L);
    assertThat(row.getReadAt()).isNotNull();
  }

  @Test
  void aSystemNoteIsFromTheRegistrarAndSaysItWasEmailed() {
    NotificationRecipient row =
        new NotificationRecipient(
            new Notification(
                null,
                Audience.PERSONAL,
                null,
                "Your account is approved",
                "Welcome!",
                Instant.now()),
            student1);
    row.setEmailOutboxId(42L);
    when(recipients.findInbox(org.mockito.ArgumentMatchers.eq(11L), any()))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(row)));

    var item = service.inbox(11L, 0, 20).items().getFirst();

    assertThat(item.senderName()).isEqualTo("Kaka Student Management");
    assertThat(item.emailed()).isTrue();
  }
}
