package com.kakastudio.sms.notification.email.event;

import com.kakastudio.sms.notification.email.service.EmailDispatcher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends a stored email as soon as the transaction that stored it has committed, on a background
 * thread so the user's request does not wait for the mail server.
 */
@Component
public class EmailDispatchListener {

  private final EmailDispatcher dispatcher;

  public EmailDispatchListener(EmailDispatcher dispatcher) {
    this.dispatcher = dispatcher;
  }

  @Async("emailExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onEmailEnqueued(EmailEnqueuedEvent event) {
    dispatcher.dispatch(event.outboxId());
  }
}
