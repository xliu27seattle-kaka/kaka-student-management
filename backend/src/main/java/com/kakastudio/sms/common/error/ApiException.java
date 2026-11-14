package com.kakastudio.sms.common.error;

import org.springframework.http.HttpStatus;

/** A failure the caller can understand: an HTTP status, a stable code and a message. */
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final ErrorCode code;

  public ApiException(HttpStatus status, ErrorCode code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public ErrorCode code() {
    return code;
  }
}
