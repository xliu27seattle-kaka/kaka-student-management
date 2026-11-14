package com.kakastudio.sms.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

  @Test
  void trimsAndLowerCases() {
    assertThat(EmailNormalizer.normalize("  Lena.Fischer@Uni.Example "))
        .isEqualTo("lena.fischer@uni.example");
  }

  @Test
  void nullStaysNull() {
    assertThat(EmailNormalizer.normalize(null)).isNull();
  }
}
