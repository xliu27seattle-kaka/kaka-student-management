package com.kakastudio.sms.notification.email.service.impl;

import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.event.EmailEnqueuedEvent;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailOutboxService;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutboxServiceImpl implements EmailOutboxService {

  private final EmailOutboxRepository repository;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  public EmailOutboxServiceImpl(
      EmailOutboxRepository repository, ApplicationEventPublisher events, Clock clock) {
    this.repository = repository;
    this.events = events;
    this.clock = clock;
  }

  @Override
  @Transactional
  public Long enqueue(EmailMessage message) {
    EmailOutbox row = repository.save(EmailOutbox.pending(message, clock.instant()));
    events.publishEvent(new EmailEnqueuedEvent(row.getId()));
    return row.getId();
  }
}
