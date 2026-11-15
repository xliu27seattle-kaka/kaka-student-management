package com.kakastudio.sms.enrollment.rules;

/** Why a student cannot enroll in a section. The UI shows the message; the code is for logic. */
public enum IneligibilityReason {
  ENROLLMENT_CLOSED,
  ACCOUNT_NOT_ELIGIBLE,
  ALREADY_ENROLLED,
  ALREADY_COMPLETED,
  COURSE_IN_PROGRESS,
  MISSING_PREREQUISITE,
  SECTION_FULL,
  SCHEDULE_CONFLICT
}
