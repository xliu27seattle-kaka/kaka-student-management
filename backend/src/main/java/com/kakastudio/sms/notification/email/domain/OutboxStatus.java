package com.kakastudio.sms.notification.email.domain;

public enum OutboxStatus {
  /** Waiting to be sent, or waiting for its next retry. */
  PENDING,
  SENT,
  /** Given up on: rejected for good, or every retry failed. */
  FAILED
}
