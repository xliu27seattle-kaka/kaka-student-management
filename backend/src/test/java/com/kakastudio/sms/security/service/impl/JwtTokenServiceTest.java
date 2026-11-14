package com.kakastudio.sms.security.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kakastudio.sms.security.JwtConfig;
import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.dto.IssuedToken;
import com.kakastudio.sms.testsupport.Users;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class JwtTokenServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

  private final JwtProperties properties =
      new JwtProperties("test-secret-test-secret-test-secret-0123456789", 120);
  private final JwtConfig config = new JwtConfig();
  private final SecretKey key = config.jwtSecretKey(properties);
  private final JwtTokenService tokenService =
      new JwtTokenService(config.jwtEncoder(key), properties, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void issuedTokenCarriesIdentityAndExpiry() {
    IssuedToken token = tokenService.issue(Users.activeStudent(7L));

    NimbusJwtDecoder decoder = (NimbusJwtDecoder) config.jwtDecoder(key);
    decoder.setJwtValidator(
        jwt -> OAuth2TokenValidatorResult.success()); // fixed clock is in the past
    Jwt jwt = decoder.decode(token.value());

    assertThat(jwt.getSubject()).isEqualTo("7");
    assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
    assertThat(jwt.getClaimAsString("email")).isEqualTo("lena.fischer@uni.example");
    assertThat(jwt.getClaimAsString("name")).isEqualTo("Lena Fischer");
    assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(120)));
    assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(120)));
  }
}
