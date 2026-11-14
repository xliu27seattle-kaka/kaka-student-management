package com.kakastudio.sms.professor.service;

public interface ProfessorNumberGenerator {

  /** The next professor number, for example {@code P-0001}. */
  String next();
}
