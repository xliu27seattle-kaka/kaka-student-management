package com.kakastudio.sms.graduation.service;

public interface CertificateNumberGenerator {

  /** For example KS-2026-0001. */
  String next(int year);
}
