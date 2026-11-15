package com.kakastudio.sms.grade.domain;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** The usual US scale: 93 and above is an A (4.0), below 60 an F (0.0). */
@Component
public class StandardGradeScale implements GradeScale {

  public static final int PASSING_SCORE = 60;

  private record Band(int minimum, String letter, String points) {}

  private static final List<Band> BANDS =
      List.of(
          new Band(93, "A", "4.0"),
          new Band(90, "A-", "3.7"),
          new Band(87, "B+", "3.3"),
          new Band(83, "B", "3.0"),
          new Band(80, "B-", "2.7"),
          new Band(77, "C+", "2.3"),
          new Band(73, "C", "2.0"),
          new Band(70, "C-", "1.7"),
          new Band(60, "D", "1.0"),
          new Band(0, "F", "0.0"));

  @Override
  public Grade gradeFor(int score) {
    if (score < 0 || score > 100) {
      throw new IllegalArgumentException("A score is between 0 and 100, not " + score);
    }
    Band band = BANDS.stream().filter(b -> score >= b.minimum()).findFirst().orElseThrow();
    return new Grade(band.letter(), new BigDecimal(band.points()));
  }

  @Override
  public boolean isPassing(int score) {
    return score >= PASSING_SCORE;
  }
}
