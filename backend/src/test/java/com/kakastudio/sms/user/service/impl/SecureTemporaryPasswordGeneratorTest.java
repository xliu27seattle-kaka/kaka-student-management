package com.kakastudio.sms.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecureTemporaryPasswordGeneratorTest {

  private final SecureTemporaryPasswordGenerator generator = new SecureTemporaryPasswordGenerator();

  @Test
  void everyPasswordIsTwelveUnambiguousCharactersWithALetterAndADigit() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 200; i++) {
      String password = generator.generate();
      assertThat(password).matches("^(?=.*[A-Za-z])(?=.*\\d)[A-HJ-NP-Za-km-z2-9]{12}$");
      seen.add(password);
    }
    assertThat(seen).hasSizeGreaterThan(190);
  }
}
