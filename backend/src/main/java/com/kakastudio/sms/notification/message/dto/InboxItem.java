package com.kakastudio.sms.notification.message.dto;

import com.kakastudio.sms.notification.message.domain.Audience;
import java.time.Instant;

/**
 * One message in a user's inbox. {@code id} is the recipient row, used to mark it read; {@code
 * emailed} says the same message also went out by email.
 */
public record InboxItem(
    Long id,
    String subject,
    String body,
    String senderName,
    Audience audience,
    String sectionCode,
    Instant createdAt,
    boolean read,
    boolean emailed) {}
