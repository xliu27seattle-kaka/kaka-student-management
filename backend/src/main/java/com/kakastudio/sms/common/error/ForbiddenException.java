package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** The caller is known but not allowed to do this (403). */
public class ForbiddenException extends ApiException {

  public ForbiddenException(ErrorCode code, String message) {
    super(HttpStatus.FORBIDDEN, code, message);
  }
}
