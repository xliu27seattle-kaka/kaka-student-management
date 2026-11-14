package com.kakastudio.sms.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.event.EmailEnqueuedEvent;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class EmailOutboxServiceImplTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  private final EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final EmailOutboxServiceImpl service =
      new EmailOutboxServiceImpl(repository, events, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void enqueueStoresAPendingRowAndAnnouncesIt() {
    when(repository.save(any()))
        .thenAnswer(invocation -> {
          EmailOutbox row = invocation.getArgument(0);
          row.setId(42L);
          return row;
        });

    Long id = service.enqueue(new EmailMessage("lena@uni.example", "Hello", "<p>Hi</p>"));

    ArgumentCaptor<EmailOutbox> saved = ArgumentCaptor.forClass(EmailOutbox.class);
    verify(repository).save(saved.capture());
    assertThat(id).isEqualTo(42L);
    assertThat(saved.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(saved.getValue().getRecipient()).isEqualTo("lena@uni.example");
    assertThat(saved.getValue().getAttempts()).isZero();
    assertThat(saved.getValue().getNextAttemptAt()).isEqualTo(NOW);
    verify(events).publishEvent(new EmailEnqueuedEvent(42L));
  }
}
