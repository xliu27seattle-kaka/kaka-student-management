package com.kakastudio.sms.security;

import org.springframework.security.core.AuthenticationException;

/** The token is genuine, but its account no longer exists or is not active any more. */
public class SessionRevokedException extends AuthenticationException {

  public SessionRevokedException() {
    super("This account is no longer active");
  }
}
