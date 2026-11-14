package com.kakastudio.sms.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.auth.dto.ChangePasswordRequest;
import com.kakastudio.sms.auth.dto.LoginRequest;
import com.kakastudio.sms.auth.dto.LoginResponse;
import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.security.dto.CurrentUser;
import com.kakastudio.sms.security.dto.IssuedToken;
import com.kakastudio.sms.security.service.CurrentUserProvider;
import com.kakastudio.sms.security.service.TokenService;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

  private static final Instant EXPIRES = Instant.parse("2026-11-13T12:00:00Z");

  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final TokenService tokens = mock(TokenService.class);
  private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
  private AuthServiceImpl service;

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode(any())).thenReturn("encoded:placeholder");
    when(passwordEncoder.matches("Passw0rd1", "encoded:Passw0rd1")).thenReturn(true);
    when(tokens.issue(any())).thenReturn(new IssuedToken("jwt-value", EXPIRES));
    service = new AuthServiceImpl(users, passwordEncoder, tokens, currentUser);
  }

  private void stored(UserStatus status) {
    User user = Users.user(7L, "lena.fischer@uni.example", Role.STUDENT, status);
    when(users.findByEmail("lena.fischer@uni.example")).thenReturn(Optional.of(user));
  }

  private ApiException loginFails(String email, String password) {
    return catchThrowableOfType(
        ApiException.class, () -> service.login(new LoginRequest(email, password)));
  }

  @Test
  void loginReturnsTokenForActiveUser() {
    stored(UserStatus.ACTIVE);

    LoginResponse response =
        service.login(new LoginRequest("lena.fischer@uni.example", "Passw0rd1"));

    assertThat(response.accessToken()).isEqualTo("jwt-value");
    assertThat(response.expiresAt()).isEqualTo(EXPIRES);
    assertThat(response.user().id()).isEqualTo(7L);
    assertThat(response.user().role()).isEqualTo(Role.STUDENT);
    assertThat(response.user().email()).isEqualTo("lena.fischer@uni.example");
  }

  @Test
  void loginNormalizesEmail() {
    stored(UserStatus.ACTIVE);

    service.login(new LoginRequest("  Lena.Fischer@Uni.Example ", "Passw0rd1"));

    verify(users).findByEmail("lena.fischer@uni.example");
  }

  @Test
  void unknownEmailIsBadCredentials() {
    when(users.findByEmail(any())).thenReturn(Optional.empty());

    ApiException thrown = loginFails("nobody@uni.example", "Passw0rd1");

    assertThat(thrown.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(thrown.code()).isEqualTo(ErrorCode.BAD_CREDENTIALS);
  }

  @Test
  void wrongPasswordIsBadCredentialsWithTheSameMessageAsUnknownEmail() {
    stored(UserStatus.ACTIVE);
    when(users.findByEmail("nobody@uni.example")).thenReturn(Optional.empty());

    ApiException wrongPassword = loginFails("lena.fischer@uni.example", "wrong");
    ApiException unknownEmail = loginFails("nobody@uni.example", "Passw0rd1");

    assertThat(wrongPassword.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(wrongPassword.code()).isEqualTo(ErrorCode.BAD_CREDENTIALS);
    assertThat(wrongPassword.getMessage()).isEqualTo(unknownEmail.getMessage());
  }

  @Test
  void pendingAccountIsRefused() {
    stored(UserStatus.PENDING);

    ApiException thrown = loginFails("lena.fischer@uni.example", "Passw0rd1");

    assertThat(thrown.status()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(thrown.code()).isEqualTo(ErrorCode.ACCOUNT_PENDING);
  }

  @Test
  void rejectedAccountIsRefused() {
    stored(UserStatus.REJECTED);

    assertThat(loginFails("lena.fischer@uni.example", "Passw0rd1").code())
        .isEqualTo(ErrorCode.ACCOUNT_REJECTED);
  }

  @Test
  void inactiveAccountIsRefused() {
    stored(UserStatus.INACTIVE);

    assertThat(loginFails("lena.fischer@uni.example", "Passw0rd1").code())
        .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
  }

  @Test
  void wrongPasswordOnPendingAccountDoesNotRevealTheStatus() {
    stored(UserStatus.PENDING);

    ApiException thrown = loginFails("lena.fischer@uni.example", "wrong");

    assertThat(thrown.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(thrown.code()).isEqualTo(ErrorCode.BAD_CREDENTIALS);
  }

  @Test
  void aPasswordTooLongForBcryptIsSimplyWrongNotACrash() {
    stored(UserStatus.ACTIVE);
    String tooLong = "x".repeat(80);

    ApiException thrown = loginFails("lena.fischer@uni.example", tooLong);

    assertThat(thrown.code()).isEqualTo(ErrorCode.BAD_CREDENTIALS);
    verify(passwordEncoder, never()).matches(eq(tooLong), any());
  }

  private User signedIn() {
    User user = Users.user(7L, "lena.fischer@uni.example", Role.STUDENT, UserStatus.ACTIVE);
    when(currentUser.require())
        .thenReturn(new CurrentUser(7L, "lena.fischer@uni.example", Role.STUDENT));
    when(users.findById(7L)).thenReturn(Optional.of(user));
    return user;
  }

  @Test
  void changePasswordStoresTheNewHash() {
    User user = signedIn();
    when(passwordEncoder.encode("NewPassw0rd2")).thenReturn("encoded:NewPassw0rd2");

    service.changePassword(new ChangePasswordRequest("Passw0rd1", "NewPassw0rd2"));

    assertThat(user.getPasswordHash()).isEqualTo("encoded:NewPassw0rd2");
  }

  @Test
  void changePasswordWithTheWrongCurrentPasswordIsRejected() {
    User user = signedIn();

    ApiException thrown =
        catchThrowableOfType(
            ApiException.class,
            () -> service.changePassword(new ChangePasswordRequest("wrong", "NewPassw0rd2")));

    assertThat(thrown.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(thrown.code()).isEqualTo(ErrorCode.CURRENT_PASSWORD_INCORRECT);
    assertThat(user.getPasswordHash()).isEqualTo("encoded:Passw0rd1");
  }

  @Test
  void changePasswordToTheSameValueIsRejected() {
    User user = signedIn();

    ApiException thrown =
        catchThrowableOfType(
            ApiException.class,
            () -> service.changePassword(new ChangePasswordRequest("Passw0rd1", "Passw0rd1")));

    assertThat(thrown.code()).isEqualTo(ErrorCode.PASSWORD_UNCHANGED);
    assertThat(user.getPasswordHash()).isEqualTo("encoded:Passw0rd1");
  }
}
