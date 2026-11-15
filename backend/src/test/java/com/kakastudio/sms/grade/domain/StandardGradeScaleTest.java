package com.kakastudio.sms.grade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StandardGradeScaleTest {

  private final StandardGradeScale scale = new StandardGradeScale();

  @ParameterizedTest
  @CsvSource({
    "100, A, 4.0", "93, A, 4.0", "92, A-, 3.7", "90, A-, 3.7", "89, B+, 3.3", "87, B+, 3.3",
    "86, B, 3.0", "83, B, 3.0", "82, B-, 2.7", "80, B-, 2.7", "79, C+, 2.3", "77, C+, 2.3",
    "76, C, 2.0", "73, C, 2.0", "72, C-, 1.7", "70, C-, 1.7", "69, D, 1.0", "60, D, 1.0",
    "59, F, 0.0", "0, F, 0.0"
  })
  void mapsEveryBoundary(int score, String letter, String points) {
    Grade grade = scale.gradeFor(score);
    assertThat(grade.letter()).isEqualTo(letter);
    assertThat(grade.points()).isEqualByComparingTo(points);
  }

  @ParameterizedTest
  @CsvSource({"60, true", "59, false"})
  void passingStartsAtSixty(int score, boolean passing) {
    assertThat(scale.isPassing(score)).isEqualTo(passing);
  }

  @ParameterizedTest
  @CsvSource({"-1", "101"})
  void refusesScoresOutsideTheRange(int score) {
    assertThatThrownBy(() -> scale.gradeFor(score)).isInstanceOf(IllegalArgumentException.class);
  }
}
