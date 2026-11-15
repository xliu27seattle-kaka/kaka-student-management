package com.kakastudio.sms.enrollment.domain;

public enum EnrollmentStatus {
  /** Holds a seat. */
  ENROLLED,
  /** Dropped while enrollment was open: no trace on the transcript, the seat is free again. */
  DROPPED,
  /** Dropped after the semester started: shows as W, not in GPA. */
  WITHDRAWN,
  /** The semester ended with a grade. */
  COMPLETED
}
