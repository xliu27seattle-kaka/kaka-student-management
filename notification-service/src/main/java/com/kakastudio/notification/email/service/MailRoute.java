package com.kakastudio.notification.email.service;

/** Which mail server an email goes through. */
public enum MailRoute {
  /** The configured SMTP account (real delivery). */
  PRIMARY,
  /** The local inbox. Never leaves the machine. */
  SANDBOX
}
