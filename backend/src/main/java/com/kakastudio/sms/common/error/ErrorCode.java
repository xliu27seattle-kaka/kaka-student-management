package com.kakastudio.sms.common.error;

/**
 * Every error code the API can return in an {@link ApiError}. The frontend maps these to messages,
 * so a code is part of the API contract: add new ones, never rename.
 */
public enum ErrorCode {
  // request level
  VALIDATION_FAILED,
  MALFORMED_REQUEST,
  INVALID_PARAMETER,
  REQUEST_FAILED,
  DATA_CONFLICT,
  /** A row another request is changing could not be locked in time; trying again works. */
  RECORD_BUSY,
  INTERNAL_ERROR,

  // authentication and access
  UNAUTHENTICATED,
  ACCESS_DENIED,
  BAD_CREDENTIALS,
  ACCOUNT_PENDING,
  ACCOUNT_REJECTED,
  ACCOUNT_INACTIVE,
  /** The token is genuine but its account was deactivated or removed after it was issued. */
  SESSION_REVOKED,
  CURRENT_PASSWORD_INCORRECT,
  PASSWORD_UNCHANGED,

  // accounts
  EMAIL_TAKEN,
  INVALID_STATUS_TRANSITION,
  STUDENT_NOT_FOUND,
  PROFESSOR_NOT_FOUND,
  /** Deactivating a person who still has enrollments or sections in an open or running semester. */
  HAS_ACTIVE_COMMITMENTS,
  /** Deleting a person or record that history depends on; deactivate or retire it instead. */
  HAS_HISTORY,

  // courses
  COURSE_NOT_FOUND,
  COURSE_CODE_TAKEN,
  COURSE_IN_USE,
  COURSE_INACTIVE,
  PREREQUISITE_NOT_FOUND,
  PREREQUISITE_SELF,
  PREREQUISITE_CYCLE,

  // semesters and sections
  SEMESTER_NOT_FOUND,
  SEMESTER_CODE_TAKEN,
  SEMESTER_NOT_EDITABLE,
  SEMESTER_IN_USE,
  SEMESTER_TRANSITION_NOT_ALLOWED,
  SEMESTER_HAS_ENROLLMENTS,
  UNGRADED_ENROLLMENTS,
  INVALID_DATE_RANGE,
  SECTION_NOT_FOUND,
  SECTION_NOT_EDITABLE,
  MEETING_INVALID,
  MEETING_OVERLAP,
  PROFESSOR_INACTIVE,
  PROFESSOR_SCHEDULE_CONFLICT,
  CAPACITY_BELOW_ENROLLED,

  // enrollment and grades
  ENROLLMENT_NOT_FOUND,
  NOT_ELIGIBLE,
  ENROLLMENT_LOCKED,
  GRADING_CLOSED,
  GRADE_TARGET_INVALID,
  NOT_YOUR_SECTION,

  // graduation
  GRADUATION_NOT_ELIGIBLE,
  GRADUATION_APPLICATION_NOT_FOUND,

  // notifications
  NOTIFICATION_NOT_FOUND,
  NO_RECIPIENTS
}
