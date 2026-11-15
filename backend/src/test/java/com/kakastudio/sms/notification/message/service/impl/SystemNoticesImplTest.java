package com.kakastudio.sms.notification.message.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.message.domain.Audience;
import com.kakastudio.sms.notification.message.domain.Notification;
import com.kakastudio.sms.notification.message.domain.NotificationRecipient;
import com.kakastudio.sms.notification.message.repository.NotificationRecipientRepository;
import com.kakastudio.sms.notification.message.repository.NotificationRepository;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SystemNoticesImplTest {

  private final EmailOutboxService outbox = mock(EmailOutboxService.class);
  private final NotificationRepository notifications = mock(NotificationRepository.class);
  private final NotificationRecipientRepository recipients =
      mock(NotificationRecipientRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private SystemNoticesImpl notices;

  private final User lena = Users.activeStudent(7L);

  @BeforeEach
  void setUp() {
    when(notifications.save(any())).thenAnswer(call -> call.getArgument(0));
    when(outbox.enqueue(any())).thenReturn(42L);
    notices =
        new SystemNoticesImpl(
            outbox,
            notifications,
            recipients,
            users,
            Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void emailsThePersonAndPutsTheNoteInTheirInboxLinkedToThatEmail() {
    notices.send(
        lena,
        new EmailMessage(lena.getEmail(), "Your account is approved", "<p>…</p>"),
        "Welcome!");

    verify(outbox).enqueue(any());
    ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(notification.capture());
    assertThat(notification.getValue().getAudience()).isEqualTo(Audience.PERSONAL);
    assertThat(notification.getValue().getSender()).isNull();
    assertThat(notification.getValue().getSubject()).isEqualTo("Your account is approved");
    assertThat(notification.getValue().getBody()).isEqualTo("Welcome!");
    ArgumentCaptor<NotificationRecipient> row =
        ArgumentCaptor.forClass(NotificationRecipient.class);
    verify(recipients).save(row.capture());
    assertThat(row.getValue().getUser()).isSameAs(lena);
    assertThat(row.getValue().getEmailOutboxId()).isEqualTo(42L);
  }

  @Test
  void administratorsGetANoteInTheirInboxButNoEmail() {
    User first = Users.activeAdmin(1L);
    User second = Users.user(2L, "registrar@kaka.example", Role.ADMIN, UserStatus.ACTIVE);
    when(users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE))
        .thenReturn(List.of(first, second));

    notices.tellAdministrators("New registration: Omar Haddad", "Omar is waiting for approval.");

    verify(outbox, never()).enqueue(any());
    ArgumentCaptor<List<NotificationRecipient>> rows = ArgumentCaptor.captor();
    verify(recipients).saveAll(rows.capture());
    assertThat(rows.getValue())
        .extracting(NotificationRecipient::getUser)
        .containsExactly(first, second);
    assertThat(rows.getValue())
        .extracting(NotificationRecipient::getEmailOutboxId)
        .containsOnlyNulls();
  }
}
