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
  PROFESSOR_NOT_FOUND
}
