package com.kakastudio.sms.notification.email.dto;

import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import java.time.Instant;

/**
 * One email as the delivery log shows it. Deliberately without the body: it may hold a temporary
 * password, and it is removed from the database once the email is sent.
 */
public record EmailLogEntry(
    Long id,
    String recipient,
    String subject,
    OutboxStatus status,
    int attempts,
    String lastError,
    Instant createdAt,
    Instant sentAt,
    Instant nextAttemptAt) {}
