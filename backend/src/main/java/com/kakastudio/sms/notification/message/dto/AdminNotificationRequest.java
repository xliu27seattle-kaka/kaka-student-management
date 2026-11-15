package com.kakastudio.sms.notification.message.dto;

import com.kakastudio.sms.common.text.Texts;
import com.kakastudio.sms.notification.message.domain.Audience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** What an administrator writes: to all students, all professors, or everyone. */
public record AdminNotificationRequest(
    @NotNull Audience audience,
    @NotBlank @Size(max = 150) String subject,
    @NotBlank @Size(max = 4000) String body) {

  public AdminNotificationRequest {
    subject = Texts.trim(subject);
    body = Texts.trim(body);
  }
}
