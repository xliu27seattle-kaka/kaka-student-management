package com.kakastudio.notification.email.service.impl;

import com.kakastudio.notification.common.MailDeliveryException;
import com.kakastudio.notification.config.AppProperties;
import com.kakastudio.notification.email.dto.EmailRequest;
import com.kakastudio.notification.email.service.EmailService;
import com.kakastudio.notification.email.service.MailRoute;
import com.kakastudio.notification.email.service.RecipientRouter;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Sends email over SMTP, through the real account or the local inbox depending on the address. */
@Service
public class SmtpEmailService implements EmailService {

  private static final int FIRST_PERMANENT_SMTP_CODE = 500;

  private final JavaMailSender primaryMailSender;
  private final JavaMailSender sandboxMailSender;
  private final RecipientRouter recipientRouter;
  private final AppProperties.Mail mail;

  public SmtpEmailService(
      @Qualifier("primaryMailSender") JavaMailSender primaryMailSender,
      @Qualifier("sandboxMailSender") JavaMailSender sandboxMailSender,
      RecipientRouter recipientRouter,
      AppProperties properties) {
    this.primaryMailSender = primaryMailSender;
    this.sandboxMailSender = sandboxMailSender;
    this.recipientRouter = recipientRouter;
    this.mail = properties.mail();
  }

  @Override
  public void send(EmailRequest request) {
    JavaMailSender sender =
        recipientRouter.route(request.to()) == MailRoute.SANDBOX
            ? sandboxMailSender
            : primaryMailSender;
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
      helper.setFrom(
          new InternetAddress(mail.from(), mail.fromName(), StandardCharsets.UTF_8.name()));
      helper.setTo(request.to());
      helper.setSubject(request.subject());
      helper.setText(request.html(), true);
      sender.send(message);
    } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
      throw new MailDeliveryException(
          "Could not send email to " + request.to() + ": " + ex.getMessage(), isPermanent(ex), ex);
    }
  }

  /** A failure is permanent when the server answered 5xx or the address itself is malformed. */
  private static boolean isPermanent(Throwable failure) {
    Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    Deque<Throwable> pending = new ArrayDeque<>();
    pending.add(failure);
    while (!pending.isEmpty()) {
      Throwable current = pending.poll();
      if (!seen.add(current)) {
        continue;
      }
      if (current instanceof AddressException
          || (current instanceof SMTPAddressFailedException address
              && address.getReturnCode() >= FIRST_PERMANENT_SMTP_CODE)
          || (current instanceof SMTPSendFailedException send
              && send.getReturnCode() >= FIRST_PERMANENT_SMTP_CODE)) {
        return true;
      }
      addIfPresent(pending, current.getCause());
      if (current instanceof MailSendException sendException) {
        sendException.getFailedMessages().values().forEach(nested -> addIfPresent(pending, nested));
      }
      if (current instanceof MessagingException messagingException) {
        addIfPresent(pending, messagingException.getNextException());
      }
    }
    return false;
  }

  private static void addIfPresent(Deque<Throwable> pending, Throwable candidate) {
    if (candidate != null) {
      pending.add(candidate);
    }
  }
}
