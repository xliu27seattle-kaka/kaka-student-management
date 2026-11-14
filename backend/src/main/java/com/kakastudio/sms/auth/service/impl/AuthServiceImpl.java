package com.kakastudio.sms.auth.service.impl;

import com.kakastudio.sms.auth.dto.ChangePasswordRequest;
import com.kakastudio.sms.auth.dto.LoginRequest;
import com.kakastudio.sms.auth.dto.LoginResponse;
import com.kakastudio.sms.auth.dto.UserSummary;
import com.kakastudio.sms.auth.service.AuthService;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.common.error.ForbiddenException;
import com.kakastudio.sms.common.error.RuleViolationException;
import com.kakastudio.sms.common.error.UnauthorizedException;
import com.kakastudio.sms.common.text.EmailNormalizer;
import com.kakastudio.sms.security.dto.IssuedToken;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.security.service.TokenService;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.repository.UserRepository;
import com.kakastudio.sms.user.validation.PasswordPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

  private static final String BAD_CREDENTIALS_MESSAGE = "Email or password is incorrect";

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokens;
  private final CurrentUserProvider currentUser;

  /** Compared against when the email is unknown, so both failures take about the same time. */
  private final String placeholderHash;

  public AuthServiceImpl(
      UserRepository users,
      PasswordEncoder passwordEncoder,
      TokenService tokens,
      CurrentUserProvider currentUser) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.currentUser = currentUser;
    this.placeholderHash = passwordEncoder.encode("placeholder-password");
  }

  @Override
  @Transactional(readOnly = true)
  public LoginResponse login(LoginRequest request) {
    User user = users.findByEmail(EmailNormalizer.normalize(request.email())).orElse(null);
    String storedHash = user == null ? placeholderHash : user.getPasswordHash();
    boolean passwordMatches = matches(request.password(), storedHash);
    // The password is checked before the status, and an unknown email gets the same answer as a
    // wrong password, so a caller cannot learn which addresses are registered or what state
    // an account is in.
    if (user == null || !passwordMatches) {
      throw new UnauthorizedException(ErrorCode.BAD_CREDENTIALS, BAD_CREDENTIALS_MESSAGE);
    }
    switch (user.getStatus()) {
      case PENDING ->
          throw new ForbiddenException(
              ErrorCode.ACCOUNT_PENDING, "Your registration is waiting for approval");
      case REJECTED ->
          throw new ForbiddenException(
              ErrorCode.ACCOUNT_REJECTED, "Your registration was not approved");
      case INACTIVE ->
          throw new ForbiddenException(
              ErrorCode.ACCOUNT_INACTIVE, "This account has been deactivated");
      case ACTIVE -> {
        // carry on
      }
    }
    IssuedToken token = tokens.issue(user);
    return new LoginResponse(token.value(), token.expiresAt(), UserSummary.of(user));
  }

  @Override
  @Transactional(readOnly = true)
  public UserSummary me() {
    return UserSummary.of(signedInUser());
  }

  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    User user = signedInUser();
    if (!matches(request.currentPassword(), user.getPasswordHash())) {
      throw new RuleViolationException(
          ErrorCode.CURRENT_PASSWORD_INCORRECT, "The current password is not correct");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new RuleViolationException(
          ErrorCode.PASSWORD_UNCHANGED, "The new password must differ from the current one");
    }
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
  }

  /** A value the hasher cannot take (more than 72 bytes) is simply not the password. */
  private boolean matches(String rawPassword, String storedHash) {
    return PasswordPolicy.fitsBcrypt(rawPassword)
        && passwordEncoder.matches(rawPassword, storedHash);
  }

  private User signedInUser() {
    return users
        .findById(currentUser.require().id())
        .orElseThrow(
            () -> new UnauthorizedException(ErrorCode.UNAUTHENTICATED, "Sign in to continue"));
  }
}
