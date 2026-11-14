package com.kakastudio.sms.notification.email.dto;

/**
 * What happened to one send attempt. {@code permanent} means trying again cannot help (the address
 * was rejected); a transient failure is worth a retry.
 */
public record EmailResult(boolean sent, boolean permanent, String error) {

  public static EmailResult ok() {
    return new EmailResult(true, false, null);
  }

  public static EmailResult transientFailure(String error) {
    return new EmailResult(false, false, error);
  }

  public static EmailResult permanentFailure(String error) {
    return new EmailResult(false, true, error);
  }
}
