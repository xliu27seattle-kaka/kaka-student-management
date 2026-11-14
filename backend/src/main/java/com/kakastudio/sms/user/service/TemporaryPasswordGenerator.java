package com.kakastudio.sms.user.service;

public interface TemporaryPasswordGenerator {

  /** A random password for an account an administrator creates on someone's behalf. */
  String generate();
}
