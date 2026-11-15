package com.kakastudio.sms.notification.email.controller;

import com.kakastudio.sms.common.paging.PageResponse;
import com.kakastudio.sms.notification.email.domain.OutboxStatus;
import com.kakastudio.sms.notification.email.dto.EmailLogEntry;
import com.kakastudio.sms.notification.email.dto.EmailSummary;
import com.kakastudio.sms.notification.email.service.EmailLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The email delivery log, for administrators. */
@RestController
@RequestMapping("/api/v1/admin/emails")
public class AdminEmailController {

  private final EmailLogService emailLog;

  public AdminEmailController(EmailLogService emailLog) {
    this.emailLog = emailLog;
  }

  @GetMapping
  public PageResponse<EmailLogEntry> list(
      @RequestParam(required = false) OutboxStatus status,
      @RequestParam(name = "q", required = false) String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return emailLog.list(status, query, page, size);
  }

  @GetMapping("/summary")
  public EmailSummary summary() {
    return emailLog.summary();
  }
}
