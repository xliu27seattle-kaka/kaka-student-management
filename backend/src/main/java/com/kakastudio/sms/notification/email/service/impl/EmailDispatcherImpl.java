package com.kakastudio.sms.notification.email.service.impl;

import com.kakastudio.sms.notification.email.config.EmailProperties;
import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailResult;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailDispatcher;
import com.kakastudio.sms.notification.email.service.EmailGateway;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailDispatcherImpl implements EmailDispatcher {

  private static final Logger log = LoggerFactory.getLogger(EmailDispatcherImpl.class);

  private final EmailOutboxRepository repository;
  private final EmailGateway gateway;
  private final EmailProperties properties;
  private final Clock clock;

  public EmailDispatcherImpl(
      EmailOutboxRepository repository,
      EmailGateway gateway,
      EmailProperties properties,
      Clock clock) {
    this.repository = repository;
    this.gateway = gateway;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void dispatch(Long outboxId) {
    EmailOutbox row = repository.findWithLockById(outboxId).orElse(null);
    if (row == null || row.getStatus() != OutboxStatus.PENDING) {
      return; // already handled by the other path (after-commit dispatch or sweep)
    }

    EmailResult result = gateway.send(row.toMessage());
    Instant now = clock.instant();
    if (result.sent()) {
      row.markSent(now);
      return;
    }

    row.recordFailure(result.error());
    boolean retriesUsedUp = row.getAttempts() > properties.maxRetries();
    if (result.permanent() || retriesUsedUp) {
      row.giveUp();
      log.warn(
          "Giving up on email {} to {} after {} attempt(s): {}",
          row.getId(),
          row.getRecipient(),
          row.getAttempts(),
          result.error());
    } else {
      // Wait a little longer after each failure: 30 s, 60 s, ...
      row.retryAt(now.plusSeconds(properties.retryDelaySeconds() * row.getAttempts()));
      log.warn(
          "Email {} to {} failed (attempt {}), will retry: {}",
          row.getId(),
          row.getRecipient(),
          row.getAttempts(),
          result.error());
    }
  }
}
