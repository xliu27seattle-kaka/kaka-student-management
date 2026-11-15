package com.kakastudio.sms.notification.email.service.impl;

import com.kakastudio.sms.common.paging.PageRequests;
import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.common.text.LikePatterns;
import com.kakastudio.sms.notification.email.domain.EmailOutbox;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailLogEntry;
import com.kakastudio.sms.notification.email.dto.EmailSummary;
import com.kakastudio.sms.notification.email.repository.EmailOutboxRepository;
import com.kakastudio.sms.notification.email.service.EmailLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EmailLogServiceImpl implements EmailLogService {

  private final EmailOutboxRepository outbox;

  public EmailLogServiceImpl(EmailOutboxRepository outbox) {
    this.outbox = outbox;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<EmailLogEntry> list(OutboxStatus status, String query, int page, int size) {
    String pattern = StringUtils.hasText(query) ? LikePatterns.contains(query) : null;
    return PageResponse.of(
        outbox.search(status, pattern, PageRequests.newestFirst(page, size)),
        EmailLogServiceImpl::toEntry);
  }

  @Override
  @Transactional(readOnly = true)
  public EmailSummary summary() {
    return new EmailSummary(
        outbox.countByStatus(OutboxStatus.PENDING),
        outbox.countByStatus(OutboxStatus.SENT),
        outbox.countByStatus(OutboxStatus.FAILED));
  }

  private static EmailLogEntry toEntry(EmailOutbox row) {
    return new EmailLogEntry(
        row.getId(),
        row.getRecipient(),
        row.getSubject(),
        row.getStatus(),
        row.getAttempts(),
        row.getLastError(),
        row.getCreatedAt(),
        row.getSentAt(),
        row.getNextAttemptAt());
  }
}
