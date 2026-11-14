package com.kakastudio.sms.security.service.impl;

import com.kakastudio.sms.security.JwtProperties;
import com.kakastudio.sms.security.dto.IssuedToken;
import com.kakastudio.sms.security.service.TokenService;
import com.kakastudio.sms.user.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

  private static final String ISSUER = "kaka-student-management";

  private final JwtEncoder jwtEncoder;
  private final Duration lifetime;
  private final Clock clock;

  public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.lifetime = Duration.ofMinutes(properties.ttlMinutes());
    this.clock = clock;
  }

  @Override
  public IssuedToken issue(User user) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(lifetime);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(expiresAt)
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .claim("name", user.fullName())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedToken(value, expiresAt);
  }
}
