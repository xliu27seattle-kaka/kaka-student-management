package com.kakastudio.sms.user.domain;

public enum UserStatus {
  /** Registered, waiting for an administrator's decision. */
  PENDING,
  ACTIVE,
  /** Registration refused. The applicant may register again. */
  REJECTED,
  /** Switched off by an administrator. Cannot sign in. */
  INACTIVE
}
