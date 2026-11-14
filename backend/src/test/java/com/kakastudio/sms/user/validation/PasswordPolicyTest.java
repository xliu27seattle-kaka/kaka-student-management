package com.kakastudio.sms.user.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

  @Test
  void acceptsEightCharactersWithALetterAndADigit() {
    assertThat(PasswordPolicy.isAcceptable("Passw0rd")).isTrue();
  }

  @Test
  void rejectsTooShortOrMissingALetterOrADigit() {
    assertThat(PasswordPolicy.isAcceptable("Pass0rd")).isFalse();
    assertThat(PasswordPolicy.isAcceptable("12345678")).isFalse();
    assertThat(PasswordPolicy.isAcceptable("Password")).isFalse();
    assertThat(PasswordPolicy.isAcceptable(null)).isFalse();
  }

  @Test
  void countsBytesNotCharactersForTheUpperLimit() {
    String seventyOneBytes =
        "\u5bc6".repeat(23) + "a1"; // 23 x 3 bytes + 2 = 71 bytes, 25 characters
    String seventyFourBytes =
        "\u5bc6".repeat(24) + "a1"; // 24 x 3 bytes + 2 = 74 bytes, 26 characters

    assertThat(PasswordPolicy.isAcceptable(seventyOneBytes)).isTrue();
    assertThat(PasswordPolicy.isAcceptable(seventyFourBytes)).isFalse();
  }
}
