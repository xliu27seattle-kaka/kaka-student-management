package com.kakastudio.sms.student.service;

public interface StudentNumberGenerator {

  /**
   * The next student number, for example {@code S2026-0001}: the registration year and a running
   * number that never repeats (it does not start again each year).
   */
  String next(int year);
}
