package com.kakastudio.sms.notification.email.service;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.dto.EmailResult;

/**
 * The way out of this application for email. The current implementation calls the notification
 * service over HTTP; a message queue could take its place without touching any caller.
 */
public interface EmailGateway {

  /** Never throws: every outcome, including a network failure, is an {@link EmailResult}. */
  EmailResult send(EmailMessage message);
}
