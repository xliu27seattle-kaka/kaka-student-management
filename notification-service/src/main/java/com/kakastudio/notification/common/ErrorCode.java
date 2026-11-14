package com.kakastudio.notification.common;

/** Every error code this service can return in an {@link ApiError}. */
public enum ErrorCode {
  VALIDATION_FAILED,
  MALFORMED_REQUEST,
  INVALID_INTERNAL_KEY,
  MAIL_REJECTED,
  MAIL_DELIVERY_FAILED,
  REQUEST_FAILED,
  INTERNAL_ERROR
}
