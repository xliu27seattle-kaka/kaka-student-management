package com.kakastudio.sms.notification.email.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailDispatcher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class EmailOutboxSweeperTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  private final EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
  private final EmailDispatcher dispatcher = mock(EmailDispatcher.class);
  private final EmailOutboxSweeper sweeper =
      new EmailOutboxSweeper(repository, dispatcher, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void dispatchesEveryDueRow() {
    when(repository.findDueIds(eq(OutboxStatus.PENDING), eq(NOW), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L));

    sweeper.sweep();

    verify(dispatcher).dispatch(1L);
    verify(dispatcher).dispatch(2L);
  }

  @Test
  void oneBrokenRowDoesNotStopTheOthers() {
    when(repository.findDueIds(eq(OutboxStatus.PENDING), eq(NOW), any(Pageable.class)))
        .thenReturn(List.of(1L, 2L));
    doThrow(new IllegalStateException("boom")).when(dispatcher).dispatch(1L);

    sweeper.sweep();

    verify(dispatcher).dispatch(2L);
  }
}
