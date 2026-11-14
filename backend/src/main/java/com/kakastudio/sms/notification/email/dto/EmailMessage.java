package com.kakastudio.sms.notification.email.dto;

/** One email, ready to send. {@code html} is the complete HTML body. */
public record EmailMessage(String to, String subject, String html) {}
