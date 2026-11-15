package com.kakastudio.sms.notification.email.service;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailLogEntry;
import com.kakastudio.sms.notification.email.dto.EmailSummary;

/** The delivery log of every email the system sent or tried to send, for administrators. */
public interface EmailLogService {

  /**
   * Newest first.
   *
   * @param status only this status, or all when {@code null}
   * @param query matched against recipient and subject, or all when blank
   */
  PageResponse<EmailLogEntry> list(OutboxStatus status, String query, int page, int size);

  EmailSummary summary();
}
