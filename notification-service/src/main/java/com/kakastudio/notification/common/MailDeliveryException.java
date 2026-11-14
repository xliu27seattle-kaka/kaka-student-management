package com.kakastudio.notification.common;

/**
 * An email could not be handed to the mail server. {@code permanent} means retrying will not help
 * (the server rejected the address); otherwise the caller may try again later.
 */
public class MailDeliveryException extends RuntimeException {

  private final boolean permanent;

  public MailDeliveryException(String message, boolean permanent, Throwable cause) {
    super(message, cause);
    this.permanent = permanent;
  }

  public boolean isPermanent() {
    return permanent;
  }
}
