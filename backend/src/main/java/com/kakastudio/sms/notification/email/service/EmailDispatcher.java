package com.kakastudio.sms.notification.email.service;

public interface EmailDispatcher {

  /**
   * Tries to send one stored email and records the outcome: sent, to be retried later, or given up
   * on. Does nothing if the email is not waiting any more.
   */
  void dispatch(Long outboxId);
}
