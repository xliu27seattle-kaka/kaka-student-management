package com.kakastudio.sms.notification.message.service.impl;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import com.kakastudio.sms.notification.message.domain.Audience;
import com.kakastudio.sms.notification.message.domain.Notification;
import com.kakastudio.sms.notification.message.domain.NotificationRecipient;
import com.kakastudio.sms.notification.message.repository.NotificationRecipientRepository;
import com.kakastudio.sms.notification.message.repository.NotificationRepository;
import com.kakastudio.sms.notification.message.service.SystemNotices;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemNoticesImpl implements SystemNotices {

  private final EmailOutboxService outbox;
  private final NotificationRepository notifications;
  private final NotificationRecipientRepository recipients;
  private final UserRepository users;
  private final Clock clock;

  public SystemNoticesImpl(
      EmailOutboxService outbox,
      NotificationRepository notifications,
      NotificationRecipientRepository recipients,
      UserRepository users,
      Clock clock) {
    this.outbox = outbox;
    this.notifications = notifications;
    this.recipients = recipients;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void send(User recipient, EmailMessage email, String note) {
    Long emailId = outbox.enqueue(email);
    Notification notification = personal(email.subject(), note, 1);
    NotificationRecipient row = new NotificationRecipient(notification, recipient);
    row.setEmailOutboxId(emailId);
    recipients.save(row);
  }

  @Override
  public void tellAdministrators(String subject, String note) {
    List<User> admins = users.findByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
    if (admins.isEmpty()) {
      return;
    }
    Notification notification = personal(subject, note, admins.size());
    recipients.saveAll(
        admins.stream().map(admin -> new NotificationRecipient(notification, admin)).toList());
  }

  private Notification personal(String subject, String note, int recipientCount) {
    Notification notification =
        notifications.save(
            new Notification(null, Audience.PERSONAL, null, subject, note, clock.instant()));
    notification.setRecipientCount(recipientCount);
    return notification;
  }
}
