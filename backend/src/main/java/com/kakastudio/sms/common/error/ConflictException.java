package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** The request clashes with the current state (409). */
public class ConflictException extends ApiException {

  public ConflictException(ErrorCode code, String message) {
    super(HttpStatus.CONFLICT, code, message);
  }
}
