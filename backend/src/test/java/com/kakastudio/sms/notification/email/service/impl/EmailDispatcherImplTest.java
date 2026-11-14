package com.kakastudio.sms.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.notification.email.config.EmailProperties;
import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.dto.EmailResult;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EmailDispatcherImplTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  private final EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
  private final EmailGateway gateway = mock(EmailGateway.class);
  private final EmailDispatcherImpl dispatcher =
      new EmailDispatcherImpl(
          repository, gateway, new EmailProperties(2, 30, 30000), Clock.fixed(NOW, ZoneOffset.UTC));

  private EmailOutbox pendingRow(int failedAttemptsSoFar) {
    EmailOutbox row =
        EmailOutbox.pending(new EmailMessage("lena@uni.example", "Hello", "<p>Hi</p>"), NOW);
    for (int i = 0; i < failedAttemptsSoFar; i++) {
      row.recordFailure("earlier failure");
    }
    when(repository.findWithLockById(5L)).thenReturn(Optional.of(row));
    return row;
  }

  @Test
  void sentRowIsMarkedSent() {
    EmailOutbox row = pendingRow(0);
    when(gateway.send(any())).thenReturn(EmailResult.ok());

    dispatcher.dispatch(5L);

    assertThat(row.getStatus()).isEqualTo(OutboxStatus.SENT);
    assertThat(row.getSentAt()).isEqualTo(NOW);
    assertThat(row.getHtml()).doesNotContain("<p>Hi</p>");
    verify(gateway).send(new EmailMessage("lena@uni.example", "Hello", "<p>Hi</p>"));
  }

  @Test
  void permanentFailureFailsImmediately() {
    EmailOutbox row = pendingRow(0);
    when(gateway.send(any())).thenReturn(EmailResult.permanentFailure("550 no such user"));

    dispatcher.dispatch(5L);

    assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(row.getAttempts()).isEqualTo(1);
    assertThat(row.getLastError()).isEqualTo("550 no such user");
    assertThat(row.getHtml()).doesNotContain("<p>Hi</p>");
  }

  @Test
  void transientFailureSchedulesARetry() {
    EmailOutbox row = pendingRow(0);
    when(gateway.send(any())).thenReturn(EmailResult.transientFailure("service down"));

    dispatcher.dispatch(5L);

    assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(row.getAttempts()).isEqualTo(1);
    assertThat(row.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(30));
  }

  @Test
  void secondTransientFailureWaitsLonger() {
    EmailOutbox row = pendingRow(1);
    when(gateway.send(any())).thenReturn(EmailResult.transientFailure("service down"));

    dispatcher.dispatch(5L);

    assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(row.getAttempts()).isEqualTo(2);
    assertThat(row.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(60));
  }

  @Test
  void givesUpAfterTheConfiguredNumberOfRetries() {
    EmailOutbox row = pendingRow(2); // the first try and one retry have already failed
    when(gateway.send(any())).thenReturn(EmailResult.transientFailure("service down"));

    dispatcher.dispatch(5L); // second and last retry

    assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(row.getAttempts()).isEqualTo(3);
  }

  @Test
  void rowThatIsNoLongerPendingIsSkipped() {
    EmailOutbox row = pendingRow(0);
    row.markSent(NOW);

    dispatcher.dispatch(5L);

    verify(gateway, never()).send(any());
  }

  @Test
  void missingRowIsIgnored() {
    when(repository.findWithLockById(9L)).thenReturn(Optional.empty());

    dispatcher.dispatch(9L);

    verify(gateway, never()).send(any());
  }

  @Test
  void aLongErrorIsCutToFitTheColumn() {
    EmailOutbox row = pendingRow(0);
    when(gateway.send(any())).thenReturn(EmailResult.transientFailure("x".repeat(900)));

    dispatcher.dispatch(5L);

    assertThat(row.getLastError()).hasSize(500);
  }
}
