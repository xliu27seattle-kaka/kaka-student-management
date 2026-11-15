package com.kakastudio.sms.notification.message.dto;

import com.kakastudio.sms.notification.message.domain.Audience;
import java.time.Instant;

/** A notification as its sender sees it, with how its emails are doing. */
public record SentNotification(
    Long id,
    Audience audience,
    String sectionCode,
    String subject,
    String body,
    Instant createdAt,
    int recipientCount,
    long emailsSent,
    long emailsPending,
    long emailsFailed) {}
