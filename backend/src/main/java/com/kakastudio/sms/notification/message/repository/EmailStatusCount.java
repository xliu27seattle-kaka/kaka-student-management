package com.kakastudio.sms.notification.message.repository;

import com.kakastudio.sms.notification.email.domain.OutboxStatus;

/** How many of a notification's emails are in one outbox status. */
public record EmailStatusCount(Long notificationId, OutboxStatus status, long total) {}
