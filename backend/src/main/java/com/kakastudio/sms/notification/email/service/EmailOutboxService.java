package com.kakastudio.sms.notification.email.service;

import com.kakastudio.sms.notification.email.dto.EmailMessage;

public interface EmailOutboxService {

  /**
   * Stores the email to be sent once the current transaction commits. Call this from inside the
   * transaction of the action the email is about: if that action rolls back, no email goes out.
   *
   * @return the id of the stored email
   */
  Long enqueue(EmailMessage message);
}
