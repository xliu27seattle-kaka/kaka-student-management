package com.kakastudio.notification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Settings of this service, bound from the {@code app.*} keys in application.yml. */
@Validated
@ConfigurationProperties("app")
public record AppProperties(@NotBlank String internalApiKey, @Valid @NotNull Mail mail) {

  public record Mail(
      @NotBlank String from,
      @NotBlank String fromName,
      @Valid @NotNull Smtp primary,
      @Valid @NotNull Smtp sandbox) {}

  public record Smtp(
      @NotBlank String host,
      @Min(1) @Max(65535) int port,
      String username,
      String password,
      boolean auth,
      boolean starttls) {}
}
