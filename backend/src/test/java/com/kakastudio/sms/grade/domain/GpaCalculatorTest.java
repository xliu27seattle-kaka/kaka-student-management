package com.kakastudio.sms.grade.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GpaCalculatorTest {

  private static GradedCredit graded(int credits, String points) {
    return new GradedCredit(credits, new BigDecimal(points));
  }

  @Test
  void weightsPointsByCreditsAndRoundsToTwoDecimals() {
    // (3 x 4.0 + 4 x 0.0 + 4 x 3.0) / 11 = 2.1818...
    assertThat(GpaCalculator.gpa(List.of(graded(3, "4.0"), graded(4, "0.0"), graded(4, "3.0"))))
        .hasValueSatisfying(gpa -> assertThat(gpa).isEqualByComparingTo("2.18"));
  }

  @Test
  void hasNoGpaWithoutGradedCredits() {
    assertThat(GpaCalculator.gpa(List.of())).isEmpty();
  }

  @Test
  void roundsHalfUp() {
    // (1 x 4.0 + 7 x 3.0) / 8 = 3.125, which rounds half up to 3.13
    assertThat(GpaCalculator.gpa(List.of(graded(1, "4.0"), graded(7, "3.0"))))
        .hasValueSatisfying(gpa -> assertThat(gpa).isEqualByComparingTo("3.13"));
  }
}
