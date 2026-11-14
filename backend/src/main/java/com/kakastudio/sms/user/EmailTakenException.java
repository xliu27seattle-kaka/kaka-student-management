package com.kakastudio.sms.user;

import com.kakastudio.sms.common.error.ConflictException;
import com.kakastudio.sms.common.error.ErrorCode;

/** Another account already uses this email address. */
public class EmailTakenException extends ConflictException {

  public static final String MESSAGE = "This email is already registered";

  public EmailTakenException() {
    super(ErrorCode.EMAIL_TAKEN, MESSAGE);
  }
}
