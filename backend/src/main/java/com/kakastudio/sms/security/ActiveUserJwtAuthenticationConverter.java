package com.kakastudio.sms.security;

import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Turns a verified token into the signed-in user. The account is looked up on every request, so a
 * user who was deactivated, rejected or deleted is locked out at once even though the token itself
 * has not expired, and a changed role takes effect immediately.
 *
 * <p>Declared as a bean in {@link SecurityConfig} instead of being component-scanned: Spring MVC
 * treats every scanned {@code Converter} as a web converter and would load this one into unrelated
 * controller tests.
 */
public class ActiveUserJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final UserRepository users;

  public ActiveUserJwtAuthenticationConverter(UserRepository users) {
    this.users = users;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    User user =
        parseId(jwt.getSubject())
            .flatMap(users::findById)
            .filter(found -> found.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(SessionRevokedException::new);
    List<SimpleGrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    return new JwtAuthenticationToken(jwt, authorities, user.getEmail());
  }

  private static Optional<Long> parseId(String subject) {
    try {
      return Optional.of(Long.valueOf(subject));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }
}
