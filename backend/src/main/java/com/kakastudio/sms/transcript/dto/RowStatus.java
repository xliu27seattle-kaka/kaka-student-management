package com.kakastudio.sms.transcript.dto;

public enum RowStatus {
  /** The semester ended with a grade; counts toward GPA. */
  GRADED,
  /** Shown as W; not in GPA. */
  WITHDRAWN,
  /** The semester has not ended; any score is provisional and not in GPA. */
  IN_PROGRESS
}
