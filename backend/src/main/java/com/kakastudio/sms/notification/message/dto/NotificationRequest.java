package com.kakastudio.sms.notification.message.dto;

import com.kakastudio.sms.common.text.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What a professor writes to a section. The body is plain text. */
public record NotificationRequest(
    @NotBlank @Size(max = 150) String subject, @NotBlank @Size(max = 4000) String body) {

  public NotificationRequest {
    subject = Texts.trim(subject);
    body = Texts.trim(body);
  }
}
