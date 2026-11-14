package com.kakastudio.sms.notification.email.config;

import java.time.Duration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NotificationClientConfig {

  public static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

  /** A RestTemplate that only talks to the notification service: base URL, key and timeouts set. */
  @Bean
  public RestTemplate notificationRestTemplate(
      RestTemplateBuilder builder, NotificationClientProperties properties) {
    return builder
        .rootUri(properties.baseUrl())
        .defaultHeader(INTERNAL_KEY_HEADER, properties.internalApiKey())
        .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
        .readTimeout(Duration.ofMillis(properties.readTimeoutMs()))
        .build();
  }
}
