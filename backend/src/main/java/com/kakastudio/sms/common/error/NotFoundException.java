package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** The thing asked for does not exist (404). */
public class NotFoundException extends ApiException {

  public NotFoundException(ErrorCode code, String message) {
    super(HttpStatus.NOT_FOUND, code, message);
  }
}
