package com.kakastudio.notification.email.service;

/** Decides which mail server a recipient's email goes through. */
public interface RecipientRouter {

  MailRoute route(String recipient);
}
