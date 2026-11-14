package com.kakastudio.sms.security.service.impl;

import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.UnauthorizedException;
import com.kakastudio.sms.security.dto.CurrentUser;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.user.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

  private static final String ROLE_PREFIX = "ROLE_";

  @Override
  public CurrentUser require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken token)) {
      throw new UnauthorizedException(ErrorCode.UNAUTHENTICATED, "Sign in to continue");
    }
    Role role =
        token.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith(ROLE_PREFIX))
            .map(authority -> Role.valueOf(authority.substring(ROLE_PREFIX.length())))
            .findFirst()
            .orElseThrow(
                () -> new UnauthorizedException(ErrorCode.UNAUTHENTICATED, "Sign in to continue"));
    return new CurrentUser(Long.valueOf(token.getToken().getSubject()), token.getName(), role);
  }
}
