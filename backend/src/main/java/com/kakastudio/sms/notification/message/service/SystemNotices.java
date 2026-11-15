package com.kakastudio.sms.notification.message.service;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.user.domain.User;

/**
 * What the system tells people about their own account: an email plus the same news in the person's
 * inbox, so nobody has to open their mailbox to know what happened. Call it inside the transaction
 * of the action it is about.
 */
public interface SystemNotices {

  /**
   * Emails the person and puts a short note in their inbox, linked to that email. The note is
   * written separately because an email may carry something the inbox must not repeat (a temporary
   * password).
   */
  void send(User recipient, EmailMessage email, String note);

  /** A note in every active administrator's inbox, without an email: something to act on. */
  void tellAdministrators(String subject, String note);
}
