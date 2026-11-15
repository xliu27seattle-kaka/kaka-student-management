package com.kakastudio.sms.notification.email.service;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.user.domain.User;

/** Writes the emails the system sends. */
public interface EmailComposer {

  EmailMessage registrationReceived(User user, String studentNo);

  EmailMessage accountApproved(User user);

  EmailMessage accountRejected(User user);

  EmailMessage accountCreated(User user, String temporaryPassword);

  EmailMessage graduationApproved(User user, String certificateNo);

  EmailMessage graduationRejected(User user);

  EmailMessage sectionAssigned(User professor, String section, String semester, String schedule);

  /** A notification an administrator or professor wrote; {@code body} is plain text. */
  EmailMessage notification(User recipient, String subject, String body, String senderName);
}
