package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** The caller could not be identified (401). */
public class UnauthorizedException extends ApiException {

  public UnauthorizedException(ErrorCode code, String message) {
    super(HttpStatus.UNAUTHORIZED, code, message);
  }
}
