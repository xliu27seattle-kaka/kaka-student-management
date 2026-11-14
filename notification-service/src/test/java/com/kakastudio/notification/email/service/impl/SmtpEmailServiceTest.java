package com.kakastudio.notification.email.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.notification.common.MailDeliveryException;
import com.kakastudio.notification.config.AppProperties;
import com.kakastudio.notification.email.dto.EmailRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailServiceTest {

  private final JavaMailSender primary = mock(JavaMailSender.class);
  private final JavaMailSender sandbox = mock(JavaMailSender.class);
  private SmtpEmailService service;

  @BeforeEach
  void setUp() {
    when(primary.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
    when(sandbox.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
    AppProperties properties =
        new AppProperties(
            "test-key",
            new AppProperties.Mail(
                "no-reply@kaka-sms.example",
                "Kaka Student Management",
                new AppProperties.Smtp("smtp.test", 587, "user", "secret", true, true),
                new AppProperties.Smtp("mailbox", 1025, "", "", false, false)));
    service =
        new SmtpEmailService(primary, sandbox, new ReservedDomainRecipientRouter(), properties);
  }

  @Test
  void sendsHtmlMessageFromConfiguredSender() throws Exception {
    service.send(new EmailRequest("someone@gmail.com", "Welcome", "<p>Hi</p>"));

    ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
    verify(primary).send(captor.capture());
    MimeMessage sent = captor.getValue();
    assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("someone@gmail.com");
    assertThat(sent.getSubject()).isEqualTo("Welcome");
    assertThat(sent.getFrom()[0].toString())
        .contains("no-reply@kaka-sms.example")
        .contains("Kaka Student Management");
    sent.saveChanges();
    assertThat(sent.getContentType()).startsWith("text/html");
    assertThat(sent.getContent().toString()).isEqualTo("<p>Hi</p>");
    verify(sandbox, never()).send(any(MimeMessage.class));
  }

  @Test
  void usesSandboxSenderForReservedDomain() {
    service.send(new EmailRequest("lena@uni.example", "Welcome", "<p>Hi</p>"));

    verify(sandbox).send(any(MimeMessage.class));
    verify(primary, never()).send(any(MimeMessage.class));
  }

  @Test
  void transientMailFailureIsNotPermanent() {
    doThrow(new MailSendException("smtp down")).when(primary).send(any(MimeMessage.class));

    MailDeliveryException thrown =
        catchThrowableOfType(
            MailDeliveryException.class,
            () -> service.send(new EmailRequest("someone@gmail.com", "Welcome", "<p>Hi</p>")));

    assertThat(thrown).isNotNull();
    assertThat(thrown.isPermanent()).isFalse();
  }

  @Test
  void rejectionWith5xxReplyIsPermanent() throws Exception {
    SMTPAddressFailedException rejected =
        new SMTPAddressFailedException(
            new InternetAddress("nobody@gmail.com"), "RCPT TO", 550, "5.1.1 no such user");
    doThrow(new MailSendException("rejected", null, Map.<Object, Exception>of("msg", rejected)))
        .when(primary)
        .send(any(MimeMessage.class));

    MailDeliveryException thrown =
        catchThrowableOfType(
            MailDeliveryException.class,
            () -> service.send(new EmailRequest("nobody@gmail.com", "Welcome", "<p>Hi</p>")));

    assertThat(thrown).isNotNull();
    assertThat(thrown.isPermanent()).isTrue();
  }
}
