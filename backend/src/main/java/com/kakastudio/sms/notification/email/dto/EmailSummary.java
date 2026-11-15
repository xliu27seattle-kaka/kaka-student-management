package com.kakastudio.sms.notification.email.dto;

/** How many emails are in each delivery status. */
public record EmailSummary(long pending, long sent, long failed) {}
