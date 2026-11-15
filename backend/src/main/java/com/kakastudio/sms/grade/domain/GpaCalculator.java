package com.kakastudio.sms.grade.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** GPA = Σ(points × credits) ÷ Σ(credits), rounded half up to two decimals. */
public final class GpaCalculator {

  private GpaCalculator() {}

  /**
   * @return empty when there are no graded credits: no GPA is not the same as a GPA of 0.00
   */
  public static Optional<BigDecimal> gpa(List<GradedCredit> attempts) {
    int credits = attempts.stream().mapToInt(GradedCredit::credits).sum();
    if (credits == 0) {
      return Optional.empty();
    }
    BigDecimal weighted =
        attempts.stream()
            .map(a -> a.points().multiply(BigDecimal.valueOf(a.credits())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Optional.of(weighted.divide(BigDecimal.valueOf(credits), 2, RoundingMode.HALF_UP));
  }
}
