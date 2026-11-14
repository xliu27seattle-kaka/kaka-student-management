package com.kakastudio.sms.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kakastudio.sms.common.error.ApiException;
import com.kakastudio.sms.common.error.ErrorCode;
import com.kakastudio.sms.testsupport.Users;
import com.kakastudio.sms.user.domain.Role;
import com.kakastudio.sms.user.domain.User;
import com.kakastudio.sms.user.domain.UserStatus;
import com.kakastudio.sms.user.repository.UserRepository;
import com.kakastudio.sms.user.service.NewAccount;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAccountServiceImplTest {

  private final UserRepository users = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final UserAccountServiceImpl service = new UserAccountServiceImpl(users, passwordEncoder);

  @BeforeEach
  void setUp() {
    when(passwordEncoder.encode(any())).thenAnswer(call -> "encoded:" + call.getArgument(0));
    when(users.save(any())).thenAnswer(call -> call.getArgument(0));
  }

  private ApiException failure(Runnable action) {
    return catchThrowableOfType(ApiException.class, action::run);
  }

  @Test
  void createStoresTheAccountWithAnEncodedPassword() {
    when(users.findByEmail("omar.haddad@uni.example")).thenReturn(Optional.empty());

    User created =
        service.create(
            new NewAccount(
                "omar.haddad@uni.example",
                "Omar",
                "Haddad",
                "Passw0rd1",
                Role.STUDENT,
                UserStatus.PENDING));

    assertThat(created.getEmail()).isEqualTo("omar.haddad@uni.example");
    assertThat(created.getPasswordHash()).isEqualTo("encoded:Passw0rd1");
    assertThat(created.getRole()).isEqualTo(Role.STUDENT);
    assertThat(created.getStatus()).isEqualTo(UserStatus.PENDING);
    verify(users).save(created);
  }

  @Test
  void createWithAnEmailThatIsTakenIsAConflict() {
    when(users.findByEmail("lena.fischer@uni.example"))
        .thenReturn(Optional.of(Users.activeStudent(7L)));

    ApiException thrown =
        failure(() ->
            service.create(
                new NewAccount(
                    "lena.fischer@uni.example",
                    "L",
                    "F",
                    "Passw0rd1",
                    Role.STUDENT,
                    UserStatus.PENDING)));

    assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(thrown.code()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    verify(users, never()).save(any());
  }

  @Test
  void changeDetailsToAnotherAccountsEmailIsAConflict() {
    User user = Users.activeStudent(7L);
    when(users.existsByEmail("taken@uni.example")).thenReturn(true);

    ApiException thrown =
        failure(() -> service.changeDetails(user, "taken@uni.example", "Lena", "Fischer"));

    assertThat(thrown.code()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    assertThat(user.getEmail()).isEqualTo("lena.fischer@uni.example");
  }

  @Test
  void changeDetailsKeepingTheSameEmailIsAllowed() {
    User user = Users.activeStudent(7L);
    when(users.existsByEmail("lena.fischer@uni.example")).thenReturn(true);

    service.changeDetails(user, "lena.fischer@uni.example", "Helena", "Fischer");

    assertThat(user.getFirstName()).isEqualTo("Helena");
  }

  @Test
  void changeStatusMovesOnlyFromTheExpectedStatus() {
    User user = Users.activeStudent(7L);

    service.changeStatus(user, UserStatus.ACTIVE, UserStatus.INACTIVE);
    assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);

    ApiException thrown =
        failure(() -> service.changeStatus(user, UserStatus.ACTIVE, UserStatus.INACTIVE));
    assertThat(thrown.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(thrown.code()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
  }

  @Test
  void changePasswordStoresTheEncodedPassword() {
    User user = Users.activeStudent(7L);

    service.changePassword(user, "NewPassw0rd2");

    assertThat(user.getPasswordHash()).isEqualTo("encoded:NewPassw0rd2");
  }
}
