package com.kakastudio.sms.notification.message.domain;

/** Who a notification goes to. Administrators pick one of the first three; professors a section. */
public enum Audience {
  ALL_STUDENTS,
  ALL_PROFESSORS,
  EVERYONE,
  SECTION
}
