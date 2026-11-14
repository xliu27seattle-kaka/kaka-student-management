package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** The request is well formed but breaks a business rule (422). */
public class RuleViolationException extends ApiException {

  public RuleViolationException(ErrorCode code, String message) {
    super(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
  }
}
