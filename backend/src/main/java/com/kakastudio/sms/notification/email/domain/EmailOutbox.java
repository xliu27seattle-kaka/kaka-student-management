package com.kakastudio.sms.notification.email.domain;

import com.kakastudio.sms.notification.email.dto.EmailMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An email waiting to be sent. The row is written in the same transaction as the action that caused
 * it, so an email is never lost between "saved" and "sent": if the application stops in between,
 * the row is still {@code PENDING} and the sweep picks it up.
 */
@Entity
@Table(name = "email_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailOutbox {

  private static final int MAX_ERROR_LENGTH = 500;

  /**
   * What replaces the body once the email is sent or given up on. A body can contain a temporary
   * password, so it is kept only for as long as it is needed.
   */
  static final String BODY_REMOVED = "(removed)";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter
  private Long id;

  @Column(nullable = false)
  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false, columnDefinition = "text")
  private String html;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OutboxStatus status;

  /** How many sends have failed so far. */
  @Column(nullable = false)
  private int attempts;

  @Column(name = "last_error")
  private String lastError;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  public static EmailOutbox pending(EmailMessage message, Instant now) {
    EmailOutbox row = new EmailOutbox();
    row.recipient = message.to();
    row.subject = message.subject();
    row.html = message.html();
    row.status = OutboxStatus.PENDING;
    row.attempts = 0;
    row.nextAttemptAt = now;
    row.createdAt = now;
    return row;
  }

  public EmailMessage toMessage() {
    return new EmailMessage(recipient, subject, html);
  }

  public void markSent(Instant now) {
    status = OutboxStatus.SENT;
    sentAt = now;
    html = BODY_REMOVED;
  }

  public void recordFailure(String error) {
    attempts++;
    lastError =
        error != null && error.length() > MAX_ERROR_LENGTH
            ? error.substring(0, MAX_ERROR_LENGTH)
            : error;
  }

  public void giveUp() {
    status = OutboxStatus.FAILED;
    html = BODY_REMOVED;
  }

  public void retryAt(Instant when) {
    nextAttemptAt = when;
  }
}
