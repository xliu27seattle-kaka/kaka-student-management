package com.kakastudio.sms.notification.email.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code app.email.*}.
 *
 * @param maxRetries how many times a failed send is tried again before the email is given up on
 * @param retryDelaySeconds wait before the first retry; each further retry waits one delay longer
 * @param sweepMs how often the background sweep looks for emails that are due
 */
@Validated
@ConfigurationProperties("app.email")
public record EmailProperties(
    @Min(0) int maxRetries, @Min(1) long retryDelaySeconds, @Min(1000) long sweepMs) {}
