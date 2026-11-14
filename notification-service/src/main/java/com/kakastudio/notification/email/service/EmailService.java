package com.kakastudio.notification.email.service;

import com.kakastudio.notification.email.dto.EmailRequest;

public interface EmailService {

  /**
   * Hands the email to a mail server.
   *
   * @throws com.kakastudio.notification.common.MailDeliveryException if the server did not take it
   */
  void send(EmailRequest request);
}
