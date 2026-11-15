package com.kakastudio.sms.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailLogEntry;
import com.kakastudio.sms.notification.email.dto.EmailMessage;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class EmailLogServiceImplTest {

  private final EmailOutboxRepository outbox = mock(EmailOutboxRepository.class);
  private final EmailLogServiceImpl service = new EmailLogServiceImpl(outbox);

  @Test
  void listsDeliveriesWithoutTheirBodies() {
    EmailOutbox row =
        EmailOutbox.pending(
            new EmailMessage(
                "lena@uni.example", "Your account", "<p>Temporary password: Secret123</p>"),
            Instant.parse("2026-11-15T10:00:00Z"));
    when(outbox.search(eq(OutboxStatus.PENDING), isNull(), any()))
        .thenReturn(new PageImpl<>(List.of(row)));

    EmailLogEntry entry = service.list(OutboxStatus.PENDING, " ", 0, 20).items().getFirst();

    assertThat(entry.recipient()).isEqualTo("lena@uni.example");
    assertThat(entry.status()).isEqualTo(OutboxStatus.PENDING);
    assertThat(EmailLogEntry.class.getRecordComponents())
        .extracting(c -> c.getName())
        .doesNotContain("html", "body");
  }

  @Test
  void searchesRecipientAndSubjectWithEscapedWildcards() {
    when(outbox.search(isNull(), eq("%50!%%"), any())).thenReturn(new PageImpl<>(List.of()));

    assertThat(service.list(null, "50%", 0, 20).items()).isEmpty();
  }
}
