package com.kakastudio.sms.notification.email.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** {@code app.notification.*}: where the notification service is and how long to wait for it. */
@Validated
@ConfigurationProperties("app.notification")
public record NotificationClientProperties(
    @NotBlank String baseUrl,
    @NotBlank String internalApiKey,
    @Min(100) int connectTimeoutMs,
    @Min(100) int readTimeoutMs) {}
