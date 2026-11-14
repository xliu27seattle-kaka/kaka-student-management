package com.kakastudio.sms.notification.email.job;

import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailDispatcher;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The safety net: every few seconds, sends the emails that are due. That covers retries and any
 * email whose after-commit dispatch never ran, for example because the application was restarted.
 */
@Component
public class EmailOutboxSweeper {

  private static final Logger log = LoggerFactory.getLogger(EmailOutboxSweeper.class);
  private static final int BATCH_SIZE = 50;

  private final EmailOutboxRepository repository;
  private final EmailDispatcher dispatcher;
  private final Clock clock;

  public EmailOutboxSweeper(
      EmailOutboxRepository repository, EmailDispatcher dispatcher, Clock clock) {
    this.repository = repository;
    this.dispatcher = dispatcher;
    this.clock = clock;
  }

  @Scheduled(
      fixedDelayString = "${app.email.sweep-ms}",
      initialDelayString = "${app.email.sweep-ms}")
  public void sweep() {
    List<Long> due =
        repository.findDueIds(OutboxStatus.PENDING, clock.instant(), PageRequest.of(0, BATCH_SIZE));
    for (Long id : due) {
      try {
        dispatcher.dispatch(id);
      } catch (RuntimeException ex) {
        log.error("Could not process outbox email {}", id, ex); // keep going with the rest
      }
    }
  }
}
