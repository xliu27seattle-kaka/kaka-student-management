package com.kakastudio.sms.student.service;

/** The student record of whoever is signed in. */
public interface CurrentStudent {

  /**
   * @throws com.kakastudio.sms.common.error.NotFoundException if the signed-in user is not a
   *     student
   */
  Long id();
}
