package com.kakastudio.notification.email.dto;

public record EmailResponse(String status) {

  public static EmailResponse sent() {
    return new EmailResponse("SENT");
  }
}
