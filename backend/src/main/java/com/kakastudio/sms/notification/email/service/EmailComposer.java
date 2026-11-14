package com.kakastudio.sms.notification.email.service;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.user.domain.User;

/** Writes the emails the system sends about accounts. */
public interface EmailComposer {

  EmailMessage registrationReceived(User user, String studentNo);

  EmailMessage accountApproved(User user);

  EmailMessage accountRejected(User user);

  EmailMessage accountCreated(User user, String temporaryPassword);
}
