package com.kakastudio.notification.common;

/** The caller did not present the shared internal key. */
public class InvalidInternalKeyException extends RuntimeException {

  public InvalidInternalKeyException() {
    super("Missing or wrong internal key");
  }
}
