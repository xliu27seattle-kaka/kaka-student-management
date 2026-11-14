package com.kakastudio.notification.config;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/** Two SMTP connections: the real one, and the local inbox for reserved test addresses. */
@Configuration
public class MailSenderConfig {

  private static final String TIMEOUT_MS = "10000";

  @Bean
  @Primary
  public JavaMailSender primaryMailSender(AppProperties properties) {
    return build(properties.mail().primary());
  }

  @Bean
  public JavaMailSender sandboxMailSender(AppProperties properties) {
    return build(properties.mail().sandbox());
  }

  private static JavaMailSender build(AppProperties.Smtp smtp) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(smtp.host());
    sender.setPort(smtp.port());
    sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
    boolean hasCredentials = StringUtils.hasText(smtp.username());
    if (hasCredentials) {
      sender.setUsername(smtp.username());
      sender.setPassword(smtp.password());
    }
    Properties mail = sender.getJavaMailProperties();
    // Credentials imply authentication, so forgetting MAIL_SMTP_AUTH cannot silently break sending.
    mail.setProperty("mail.smtp.auth", String.valueOf(smtp.auth() || hasCredentials));
    mail.setProperty("mail.smtp.starttls.enable", String.valueOf(smtp.starttls()));
    mail.setProperty("mail.smtp.connectiontimeout", TIMEOUT_MS);
    mail.setProperty("mail.smtp.timeout", TIMEOUT_MS);
    mail.setProperty("mail.smtp.writetimeout", TIMEOUT_MS);
    return sender;
  }
}
