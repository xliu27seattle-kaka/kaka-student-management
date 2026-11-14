package com.kakastudio.sms.notification.email.event;

/** Published inside the transaction that stored the email; handled only after it commits. */
public record EmailEnqueuedEvent(Long outboxId) {}
