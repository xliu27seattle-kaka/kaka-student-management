package com.kakastudio.sms.notification.message.domain;

/**
 * Who a notification goes to. Administrators pick one of the first three, professors a section, and
 * the system writes PERSONAL notes about one person's own account.
 */
public enum Audience {
  ALL_STUDENTS,
  ALL_PROFESSORS,
  EVERYONE,
  SECTION,
  /** A system note to one person (account approved, graduation decided, section assigned...). */
  PERSONAL
}
